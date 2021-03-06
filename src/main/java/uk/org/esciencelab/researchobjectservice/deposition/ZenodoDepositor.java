package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * A Depositor to deposit a zipped BagIt-RO serialization of a Research Object into Zenodo through the depositions API.
 */
@Component
public class ZenodoDepositor implements Depositor {
    private static final Logger logger = LoggerFactory.getLogger(ZenodoDepositor.class);

    @Autowired
    private ZenodoDepositorConfig config;

    @Autowired
    private BagItROService bagItROService;

    public ZenodoDepositor() { }

    public URI deposit(ResearchObject researchObject, Map<String, String> params) throws DepositionException {
        try {
            File tempFile = File.createTempFile("zenodo-payload", ".zip");
            FileOutputStream os = new FileOutputStream(tempFile);
            bagItROService.bagToZip(researchObject, os);

            ZenodoClient client = new ZenodoClient(config.getApiUrl(), config.getAccessToken());
            logger.info("Creating Zenodo deposition.");
            JsonNode depositionResponse = client.createDeposition(buildMetadata(researchObject));
            int depositionId = depositionResponse.get("id").asInt();

            logger.info("Uploading Zenodo deposition file.");
            client.createDepositionFile(tempFile, depositionId, researchObject.getFriendlyId() + ".zip");

            logger.info("Publishing deposition.");
            JsonNode pubRes = client.publishDeposition(depositionId);
            logger.info(pubRes.toString());

            URI depositionUrl = new URI(pubRes.get("links").get("record").asText());
            return depositionUrl;
        } catch (DepositionException e) { // Don't double wrap
            logger.error("Deposition error:", e);
            throw e;
        } catch (Exception e) {
            logger.error("Deposition error:", e);
            throw new DepositionException(e);
        }
    }

    private JsonNode buildMetadata(ResearchObject researchObject) throws Exception {
        ObjectMapper om = new ObjectMapper();
        ObjectNode meta;
        try {
            meta = (ObjectNode) om.readTree(researchObject.getField("_metadata").toString());
        } catch (IOException e) {
            throw new DepositionException("Could not duplicate metadata.");
        }
        if (meta == null)
            throw new DepositionException("No '_metadata' field provided!");

        // Zenodo doesn't like the full URI in ORCIDs
        if (meta.has("creators")) {
            ArrayNode creators = (ArrayNode) meta.get("creators");
            for (JsonNode jcreator : creators) {
                ObjectNode creator = (ObjectNode) jcreator;
                if (creator.has("orcid")) {
                    String strippedOrcid = creator.get("orcid").asText().replaceAll("https?://orcid\\.org/", "");
                    creator.put("orcid", strippedOrcid);
                }
            }
        }

        meta.put("upload_type", "dataset");
        meta.put("version", researchObject.computeContentSha256());
        meta.put("publication_date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        if (!meta.has("access_right")) {
            meta.put("access_right", "closed");
        }

        ObjectNode node = om.createObjectNode();
        node.set("metadata", meta);

        return node;
    }
}
