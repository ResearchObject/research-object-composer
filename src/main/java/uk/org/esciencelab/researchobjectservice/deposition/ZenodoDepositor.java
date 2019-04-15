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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ZenodoDepositor implements Depositor {
    private static final Logger logger = LoggerFactory.getLogger(ZenodoDepositor.class);

    @Autowired
    private ZenodoDepositorConfig config;

    @Autowired
    private BagItROService bagItROService;

    public ZenodoDepositor() { }

    public URI deposit(ResearchObject researchObject) throws DepositionException {
        try {
            ZenodoClient client = new ZenodoClient(config.getApiUrl(), config.getAccessToken());
            logger.info("Creating Zenodo deposition.");
            JsonNode depositionResponse = client.createDeposition(buildMetadata(researchObject));

            File tempFile = File.createTempFile("zenodo-payload", ".zip");
            FileOutputStream os = new FileOutputStream(tempFile);
            bagItROService.bagToZip(researchObject, os);

            logger.info("Uploading Zenodo deposition file.");
            JsonNode depositionFileResponse = client.createDepositionFile(tempFile,
                    depositionResponse.get("id").asInt(),
                    researchObject.getFriendlyId() + ".zip");

            return new URI(depositionFileResponse.get("links").get("self").asText());
        } catch (DepositionException e) { // Don't double wrap
            throw e;
        } catch (Exception e) {
            throw new DepositionException(e);
        }
    }

    private JsonNode buildMetadata(ResearchObject researchObject) throws Exception {
        ObjectNode meta = (ObjectNode) researchObject.getField("_metadata");
        if (meta == null)
            throw new DepositionException("No '_metadata' field provided!");

        ObjectMapper mapper = new ObjectMapper();
        meta.put("upload_type", "dataset");
        meta.put("version", researchObject.computeContentSha256());
        meta.put("access_right", "closed");
        meta.put("publication_date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        ObjectNode node = mapper.createObjectNode();
        node.set("metadata", meta);

        return node;
    }
}
