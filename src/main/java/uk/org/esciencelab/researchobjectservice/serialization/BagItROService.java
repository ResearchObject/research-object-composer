package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
        BagItRO bag = new BagItRO(bagLocation);

        // Write the RO's JSON content into the temp dir, so it will be automatically bagged by the BagCreator
        ObjectMapper mapper = new ObjectMapper();
        bag.addData("content.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(researchObject.getContent()));

        // Traverse through the RO content and gather up all the remote files that are to be referenced in fetch.txt
        ArrayList<RemoteResource> entries = gatherBagEntries(researchObject.getContent(),
                researchObject.getProfile().getSchemaWrapper().getObjectSchema(), null);

        for (RemoteResource entry : entries) {
            bag.addRemote(entry);
        }

        new BagItROWriter(bag).write();

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
     * A recursive method to traverse a JSON object, discovering and gathering a list of RemoteResource objects that should
     * be bagged in the BagIt RO.
     *
     * @param json The JSON object to traverse.
     * @param schema A schema for the JSON object.
     * @param bagPath The path (relative to bagRoot) where to bag the next JSON object that is discovered.
     *                Should be null to start with.
     */
    public ArrayList<RemoteResource> gatherBagEntries(JsonNode json, Schema schema, String bagPath) {
        HashMap<String, String> baggableMap = (HashMap<String, String>) schema.getUnprocessedProperties().get("$baggable");
        ArrayList<RemoteResource> entries = new ArrayList<>();

        if (json.isArray()) {
            Schema itemSchema = ((ArraySchema) schema).getAllItemSchema();
            if (itemSchema != null) {
                for (JsonNode child : json) {
                    if (child.isContainerNode()) {
                        entries.addAll(gatherBagEntries(child, itemSchema, bagPath));
                    }
                }
            }
        } else if (json.isObject()) {
            // Bag this thing if bagPath was set!
            if (bagPath != null) {
                try {
                    entries.add(new RemoteResource(bagPath, json));
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
                        entries.addAll(gatherBagEntries(entry.getValue(), propertySchema, newBagPath));
                    }
                }
            }
        }

        return entries;
    }

    private void zipTo(Path sourceFile, Path zipDestination, ZipOutputStream zipOutputStream) {
        try {
            if (sourceFile.toFile().isDirectory()) {
                return;
            }
            InputStream inputStream = Files.newInputStream(sourceFile);

            ZipEntry zipEntry = new ZipEntry(zipDestination.toString());

            zipOutputStream.putNextEntry(zipEntry);

            IOUtils.copy(inputStream, zipOutputStream);

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
