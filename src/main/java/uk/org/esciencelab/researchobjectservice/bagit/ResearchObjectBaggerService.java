package uk.org.esciencelab.researchobjectservice.bagit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ResearchObjectBaggerService {

    public Path bag(ResearchObject researchObject) throws Exception {
        // Create a (unique) temp directory to hold the various BagIt files
        Path bagLocation = Files.createTempDirectory("bag");

        // Write the RO's JSON content into the temp dir
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
        ArrayList<BagEntry> entries = gatherBagEntries(new ArrayList<>(), bagLocation,
                researchObject.getContent(),
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
        Set<Manifest> manifests = bag.getPayLoadManifests();
        for (Manifest manifest : manifests) {
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
    public ArrayList<BagEntry> gatherBagEntries(ArrayList<BagEntry> entries, Path basePath, JsonNode json, Schema schema, String bagPath) {
        HashMap<String, String> baggableMap = (HashMap<String, String>) schema.getUnprocessedProperties().get("$baggable");

        if (json.isArray()) {
            Schema itemSchema = ((ArraySchema) schema).getAllItemSchema();
            for (JsonNode child : json) {
                if (child.isContainerNode()) {
                    if (baggableMap != null) {
                        try {
                            entries.add(buildBagEntry(basePath.resolve(bagPath), child));
                        } catch (MalformedURLException e) {
                            System.err.println("Bad URL:");
                            System.err.println(child);
                        }
                    }

                    gatherBagEntries(entries, basePath, child, itemSchema, bagPath);
                }
            }
        } else if (json.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> i = json.fields();
            while (i.hasNext()) {
                Map.Entry<String, JsonNode> entry = i.next();
                if (entry.getValue().isContainerNode()) {
                    Schema propertySchema = ((ObjectSchema) resolveSchema(schema)).getPropertySchemas().get(entry.getKey());

                    String newBagPath = null;
                    if (baggableMap != null) {
                        newBagPath = baggableMap.get(entry.getKey());
                        if (newBagPath != null) {
                            newBagPath = "data" + newBagPath;
                        }
                    }
                    gatherBagEntries(entries, basePath, entry.getValue(), propertySchema, newBagPath);
                }
            }
            if (bagPath != null) {
                try {
                    entries.add(buildBagEntry(basePath.resolve(bagPath), json));
                } catch (MalformedURLException e) {
                    System.err.println("Bad URL:");
                    System.err.println(json);
                }
            }
        }

        return entries;
    }

    private BagEntry buildBagEntry(Path basePath, JsonNode json) throws MalformedURLException {
        BagEntry b = new BagEntry(basePath, json);
        for (JsonNode checksumNode : json.get("checksums")) {
            if (checksumNode.get("type").asText().equals("md5")) {
                b.setChecksum(StandardSupportedAlgorithms.MD5, checksumNode.get("checksum").asText());
            } else if (checksumNode.get("type").asText().equals("sha256")) {
                b.setChecksum(StandardSupportedAlgorithms.SHA256, checksumNode.get("checksum").asText());
            } else if (checksumNode.get("type").asText().equals("sha512")) {
                b.setChecksum(StandardSupportedAlgorithms.SHA512, checksumNode.get("checksum").asText());
            }
        }

        return b;
    }

    private Schema resolveSchema(Schema schema) {
        while (schema instanceof ReferenceSchema) {
            schema = ((ReferenceSchema) schema).getReferredSchema();
        }

        return schema;
    }
}
