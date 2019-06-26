package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.domain.Metadata;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.util.PathUtils;
import gov.loc.repository.bagit.writer.BagWriter;
import gov.loc.repository.bagit.writer.ManifestWriter;
import gov.loc.repository.bagit.writer.MetadataWriter;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A service for creating a BagIt RO folder (or Zip file) from a given ResearchObject.
 */
@Service
public class BagItROService {
    private static final Logger logger = LoggerFactory.getLogger(BagItROService.class);

    /**
     * Create a BagIt RO folder.
     * @param researchObject The Research Object to bag.
     * @return The path to the folder.
     * @throws Exception
     */
    public Path bag(ResearchObject researchObject) throws Exception {
        logger.info("Bagging Research Object.");
        // Create a (unique) temp directory to hold the various BagIt files
        Path bagLocation = Files.createTempDirectory("bag");

        // directory to hold the RO manifest
        Path roMetadataLocation = bagLocation.resolve("metadata");
        BagItROManifest roManifest = new BagItROManifest(roMetadataLocation);
        roManifest.setId(URI.create("../"));

        // Write the RO's JSON content into the temp dir, so it will be automatically bagged by the BagCreator
        ObjectMapper mapper = new ObjectMapper();
        Files.write(bagLocation.resolve("content.json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(researchObject.getContent()));

        // Create a map of maps (checksum type -> (file -> checksum)), to hold the checksums of the remote files referenced
        //  by the RO, grouped by checksum type.
        Map<SupportedAlgorithm, Map<Path, String>> checksumMap = new HashMap<>(3);
        checksumMap.put(StandardSupportedAlgorithms.MD5, new HashMap<>());
        checksumMap.put(StandardSupportedAlgorithms.SHA256, new HashMap<>());
        checksumMap.put(StandardSupportedAlgorithms.SHA512, new HashMap<>());

        // Create a new bag in our temp dir, should originally just contain the RO's content in data/content.json
        Bag bag = BagCreator.bagInPlace(bagLocation, checksumMap.keySet(), false);

        // Traverse through the RO content and gather up all the remote files that are to be referenced in fetch.txt
        ArrayList<BagEntry> entries = new ArrayList<>();
        gatherBagEntries(entries, bagLocation, researchObject.getContent(),
                researchObject.getProfile().getSchemaWrapper().getObjectSchema(), null);

        // For each remote file found, check which checksums are used and add to the respective map.
        // Also track total octet count of all remote files;
        int octetCount = 0;
        int streamCount = 0;

        for (BagEntry entry : entries) {
            for (SupportedAlgorithm alg : checksumMap.keySet()) {
                String checksum = entry.getChecksum(alg);
                if (checksum != null) {
                    checksumMap.get(alg).put(entry.getFilepath(), checksum);
                }
            }

            octetCount += entry.getLength();
            streamCount++;
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

        // Hack the "Payload-Oxum" in bag-info.txt because it doesn't include the remote fetch.txt items in its sum.
        Metadata bagitMetadata = bag.getMetadata();
        String payloadOxum = bagitMetadata.get("Payload-Oxum").get(0);
        String [] parts = payloadOxum.split("\\.");
        octetCount += Integer.parseInt(parts[0]);
        streamCount += Integer.parseInt(parts[1]);
        bagitMetadata.upsertPayloadOxum("" + octetCount + "." + streamCount);
        bag.setMetadata(bagitMetadata); // Not sure this is needed...

        // Re-write the metadata file with the updated Oxum.
        Path bagitDir = PathUtils.getBagitDir(bag);
        MetadataWriter.writeBagMetadata(bagitMetadata, bag.getVersion(), bagitDir, bag.getFileEncoding());

        // Re-write the tag manifests to account for changes to bagit-info.txt
        Path bagInfo = bagitDir.resolve("bag-info.txt");
        Hasher.hash(bagInfo, tagManifestToDigestMap);
        Set<Manifest> updatedTagManifests = tagManifestToDigestMap.keySet();
        bag.setTagManifests(updatedTagManifests);
        ManifestWriter.writeTagManifests(updatedTagManifests, bagitDir, bagLocation, bag.getFileEncoding());

        return bagLocation;
    }

    /**
     * Bag the given Research Object, then Zip it to the given output stream.
     * @param researchObject The RO to bag.
     * @param outputStream The stream to zip to.
     * @throws Exception
     */
    public void bagToZip(ResearchObject researchObject, OutputStream outputStream) throws Exception {
        Path bagLocation = bag(researchObject); // Create the bag
        logger.info("Zipping Research Object.");
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream); // Prep the stream
        // Zip each file in the bag
        Files.walk(bagLocation).forEach(source -> zipTo(source, bagLocation.relativize(source), zipOutputStream));
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    /**
     * A recursive method to traverse a JSON object, discovering and gathering a list of BagEntry objects that should
     * be bagged in the BagIt RO.
     *
     * @param entries The list of entries to be populated.
     * @param bagRoot A path to the root of the bag.
     * @param json The JSON object to traverse.
     * @param schema A schema for the JSON object.
     * @param bagPath The path (relative to bagRoot) where to bag the next JSON object that is discovered.
     *                Should be null to start with.
     */
    public void gatherBagEntries(ArrayList<BagEntry> entries, Path bagRoot, JsonNode json, Schema schema, String bagPath) {
        HashMap<String, String> baggableMap = (HashMap<String, String>) schema.getUnprocessedProperties().get("$baggable");

        if (json.isArray()) {
            Schema itemSchema = ((ArraySchema) schema).getAllItemSchema();
            if (itemSchema != null) {
                for (JsonNode child : json) {
                    if (child.isContainerNode()) {
                        gatherBagEntries(entries, bagRoot, child, itemSchema, bagPath);
                    }
                }
            }
        } else if (json.isObject()) {
            // Bag this thing if bagPath was set!
            if (bagPath != null) {
                try {
                    entries.add(new BagEntry(bagRoot, bagPath, json));
                } catch (MalformedURLException e) {
                    logger.error("Could not bag malformed URL: " + json.toString());
                }
            }
            Iterator<Map.Entry<String, JsonNode>> i = json.fields();
            while (i.hasNext()) {
                Map.Entry<String, JsonNode> entry = i.next();
                if (entry.getValue().isContainerNode()) {
                    Schema propertySchema = ((ObjectSchema) resolveSchema(schema)).getPropertySchemas().get(entry.getKey());

                    if (propertySchema != null) {
                        // If this property has an entry in the $baggable map, set up bagPath.
                        String newBagPath = null;
                        if (baggableMap != null) {
                            newBagPath = baggableMap.get(entry.getKey());
                        }
                        gatherBagEntries(entries, bagRoot, entry.getValue(), propertySchema, newBagPath);
                    }
                }
            }
        }
    }


    private void zipTo(Path sourceFile, Path zipDestination, ZipOutputStream zipOutputStream) {
        try {
            if (sourceFile.toFile().isDirectory()) {
                return;
            }
            InputStream inputStream = Files.newInputStream(sourceFile);

            ZipEntry zipEntry = new ZipEntry(zipDestination.toString());

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

    private Schema resolveSchema(Schema schema) {
        while (schema instanceof ReferenceSchema) {
            schema = ((ReferenceSchema) schema).getReferredSchema();
        }

        return schema;
    }
}
