package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A class for writing all necessary tag files etc. for a given Bag as a BagIt + RO crate, with ro-crate-metadata.jsonld.
 */
public class BagItROCrateWriter extends BagWriter {
    private ObjectNode metadata;
    private static final Logger logger = LoggerFactory.getLogger(BagItROCrateWriter.class);

    /**
     * Create a new RO crate writer for the given bag, with the given metadata to be converted and written to ro-crate-metadata.jsonld.
     * @param bag The bag to write.
     * @param metadata The RO metadata, usually held under a top-level property named `_metadata`.
     */
    public BagItROCrateWriter(Bag bag, ObjectNode metadata) {
        super(bag);
        this.metadata = metadata;
    }

    @Override
    protected void writeTagFiles() throws IOException, NoSuchAlgorithmException {
        writeROCrateMetadata();
        super.writeTagFiles();
    }

    private void writeROCrateMetadata() throws IOException, NoSuchAlgorithmException {
        logger.info("Writing ro-crate-metadata.jsonld");
        Path path = bag.getLocation().resolve("ro-crate-metadata.jsonld");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), bag.getSupportedAlgorithms());
        ROCrateMetadata crateMetadata = new ROCrateMetadata();
        for (RemoteResource resource : bag.getRemoteItems()) {
            crateMetadata.addFile(resource.getFilepath().toString(),
                    resource.getFilename(),
                    resource.getUrl(),
                    resource.getLength());
        }

        if (metadata.has("creators")) {
            ArrayNode creators = (ArrayNode) metadata.get("creators");
            for (JsonNode creator : creators) {
                crateMetadata.addAuthor(creator.get("name").asText());
            }
        }

        if (metadata.has("title")) {
            crateMetadata.setName(metadata.get("title").asText());
        }

        if (metadata.has("description")) {
            crateMetadata.setName(metadata.get("title").asText());
        }

        crateMetadata.setDatePublished(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).writerWithDefaultPrettyPrinter().writeValue(stream, crateMetadata);

        updateChecksumMap(tagFileChecksums, stream, path);
    }
}
