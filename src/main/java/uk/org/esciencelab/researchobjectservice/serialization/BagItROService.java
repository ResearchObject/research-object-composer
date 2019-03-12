package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.writer.BagWriter;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.springframework.stereotype.Service;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BagItROService {

    public Path bag(ResearchObject researchObject) throws Exception {
        // Create a (unique) temp directory to hold the various BagIt files
        Path bagLocation = Files.createTempDirectory("bag");

        // directory to hold the RO manifest
        Path roMetadataLocation = bagLocation.resolve("manifest");
        BagItROManifest roManifest = new BagItROManifest(roMetadataLocation);
        roManifest.setId(URI.create("../"));

        // Write the RO's JSON content into the temp dir, so it will be automatically bagged by the BagCreator
        ObjectMapper mapper = new ObjectMapper();
        Files.write(bagLocation.resolve("content.json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(researchObject.getContent()));

        // Create a map of maps (checksum type -> (file -> checksum)), to hold the checksums of the remote files referenced
        //  by the RO, grouped by checksum type.
        Map<SupportedAlgorithm, Map<Path, String>> checksumMap = new HashMap<>();
        checksumMap.put(StandardSupportedAlgorithms.MD5, new HashMap<>());
        checksumMap.put(StandardSupportedAlgorithms.SHA256, new HashMap<>());
        checksumMap.put(StandardSupportedAlgorithms.SHA512, new HashMap<>());

        // Create a new bag in our temp dir, should originally just contain the RO's content in data/content.json
        Bag bag = BagCreator.bagInPlace(bagLocation, checksumMap.keySet(), false);

        // Traverse through the RO content and gather up all the remote files that are to be referenced in fetch.txt
        ArrayList<BagEntry> entries = new ArrayList<>();
        gatherBagEntries(bag, entries, bagLocation, researchObject.getContent(),
                researchObject.getProfile().getSchemaWrapper().getObjectSchema(), null);

        // For each remote file found, check which checksums are used and add to the respective map
        for (BagEntry entry : entries) {
            for (SupportedAlgorithm alg : checksumMap.keySet()) {
                String checksum = entry.getChecksum(alg);
                if (checksum != null) {
                    checksumMap.get(alg).put(entry.getFilepath(), checksum);
                }
            }
        }

        // Merge remote file checksums into the existing payload manifests (which should each just contain the one entry for data/content.json)
        for (Manifest manifest : bag.getPayLoadManifests()) {
            SupportedAlgorithm alg = manifest.getAlgorithm();
            Map<Path, String> map = checksumMap.get(alg);
            if (map != null && !map.isEmpty()) {
                Map<Path, String> existingMap = manifest.getFileToChecksumMap();
                Iterator<Map.Entry<Path, String>> i = map.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<Path, String> entry = i.next();
                    existingMap.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Set the fetch items
        bag.setItemsToFetch(entries.stream().map(BagEntry::getFetchItem).collect(Collectors.toList()));

        // Populate the RO manifest
        roManifest.setAggregates(entries.stream().map(BagEntry::getPathMetadata).collect(Collectors.toList()));
        Path roManifestPath = roManifest.writeAsJsonLD();

        // Write the checksums of the RO manifest (metadata/manifest.json) into the respective BagIt tag manifests
        Map<Manifest, MessageDigest> tagManifestToDigestMap = new HashMap<>();
        for (Manifest manifest : bag.getTagManifests()) {
            tagManifestToDigestMap.put(manifest, MessageDigest.getInstance(manifest.getAlgorithm().getMessageDigestName()));
        }

        // TODO: This code currently assumes there will only be the 1 file (manifest.json) under metadata/ !
        Hasher.hash(roManifestPath, tagManifestToDigestMap);

        // Write everything into the temp dir (payload manifests, tag manifests, fetch file)
        BagWriter.write(bag, bagLocation);

        return bagLocation;
    }

    public void bagToZip(ResearchObject researchObject, OutputStream outputStream) throws Exception {
        Path bagLocation = bag(researchObject);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        // TODO: Got to be a better way of doing this path manipulation
        Files.walk(bagLocation)
                .forEach(source -> zipTo(source, researchObject.getId() + "/" + bagLocation.relativize(source).toString(), zipOutputStream));
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    private void zipTo(Path source, String entryName, ZipOutputStream zipOutputStream) {
        try {
            if (source.toFile().isDirectory()) {
                return;
            }
            InputStream inputStream = Files.newInputStream(source);

            ZipEntry zipEntry = new ZipEntry(entryName);

            zipOutputStream.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;

            while ((length = inputStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
            zipOutputStream.closeEntry();
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // A recursive method to traverse a JSON object
    public void gatherBagEntries(Bag bag, ArrayList<BagEntry> entries, Path basePath, JsonNode json, Schema schema, String bagPath) {
        HashMap<String, String> baggableMap = (HashMap<String, String>) schema.getUnprocessedProperties().get("$baggable");

        if (json.isArray()) {
            Schema itemSchema = ((ArraySchema) schema).getAllItemSchema();
            for (JsonNode child : json) {
                if (child.isContainerNode()) {
                    gatherBagEntries(bag, entries, basePath, child, itemSchema, bagPath);
                }
            }
        } else if (json.isObject()) {
            // Bag this thing if bagPath was set!
            if (bagPath != null) {
                try {
                    entries.add(new BagEntry(bag, basePath.resolve(bagPath + "/"), json));
                } catch (MalformedURLException e) {
                    System.err.println("Bad URL:");
                    System.err.println(json);
                }
            }
            Iterator<Map.Entry<String, JsonNode>> i = json.fields();
            while (i.hasNext()) {
                Map.Entry<String, JsonNode> entry = i.next();
                if (entry.getValue().isContainerNode()) {
                    Schema propertySchema = ((ObjectSchema) resolveSchema(schema)).getPropertySchemas().get(entry.getKey());

                    // If this property has an entry in the $baggable map, set up bagPath.
                    String newBagPath = null;
                    if (baggableMap != null) {
                        newBagPath = baggableMap.get(entry.getKey());
                        if (newBagPath != null) {
                            newBagPath = "data" + newBagPath;
                        }
                    }
                    gatherBagEntries(bag, entries, basePath, entry.getValue(), propertySchema, newBagPath);
                }
            }
        }
    }

    private Schema resolveSchema(Schema schema) {
        while (schema instanceof ReferenceSchema) {
            schema = ((ReferenceSchema) schema).getReferredSchema();
        }

        return schema;
    }
}
