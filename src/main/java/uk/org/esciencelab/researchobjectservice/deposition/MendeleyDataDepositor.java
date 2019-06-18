package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;
import uk.org.esciencelab.researchobjectservice.serialization.BagItROService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * A Depositor to deposit a zipped BagIt-RO serialization of a Research Object into Mendeley Data through the datasets API.
 */
@Component
public class MendeleyDataDepositor implements Depositor {
    private static final Logger logger = LoggerFactory.getLogger(MendeleyDataDepositor.class);
    private static final String [] metadataFields = { "name", "description", "publish_date", "categories", "contributors", "data_licence", "articles" };

    @Autowired
    private MendeleyDataDepositorConfig config;

    @Autowired
    private BagItROService bagItROService;

    public MendeleyDataDepositor() { }

    public URI deposit(ResearchObject researchObject, Map<String, String> params) throws DepositionException {
        try {
            File tempFile = File.createTempFile("md-payload", ".zip");
            FileOutputStream os = new FileOutputStream(tempFile);
            bagItROService.bagToZip(researchObject, os);

            MendeleyDataClient client = new MendeleyDataClient(config.getApiUrl(), params.get("accessToken"));

            logger.info("Creating Mendeley Data Dataset.");
            JsonNode datasetResponse = client.createDataset(buildMetadata(researchObject));
            logger.info(datasetResponse.toString());
            String datasetId = datasetResponse.get("id").asText();

            logger.info("Uploading Mendeley Data File Content.");
            JsonNode fileContentResponse = client.createFileContent(tempFile);
            logger.info(fileContentResponse.toString());
            String fileContentId = fileContentResponse.get("id").asText();

            logger.info("Linking File Content (id: " + fileContentId + ") to Dataset (id: " + datasetId + ").");
            JsonNode patchResponse = client.addFileToDataset(datasetId, fileContentId, researchObject.getFriendlyId() + ".zip", "Research Object");
            logger.info(patchResponse.toString());

            return new URI("https://doi.org/" + patchResponse.get("doi").get("id").asText());
        } catch (DepositionException e) { // Don't double wrap
            logger.error("Deposition error:", e);
            throw e;
        } catch (Exception e) {
            logger.error("Deposition error:", e);
            throw new DepositionException(e);
        }
    }

    private JsonNode buildMetadata(ResearchObject researchObject) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode meta;
        try {
            meta = (ObjectNode) om.readTree(researchObject.getField("_metadata").toString());
        } catch (IOException e) {
            throw new DepositionException("Could not duplicate metadata.");
        }
        if (meta == null)
            throw new DepositionException("No '_metadata' field provided!");

        // Rename some fields
        if (!meta.has("name") && meta.has("title"))
            meta.set("name", meta.remove("title"));

        if (!meta.has("contributors") && meta.has("creators"))
            meta.set("contributors", meta.remove("creators"));

        return meta.retain(metadataFields);
    }
}
