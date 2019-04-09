package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.cs.swt.zenodo.Metadata;
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

    @Autowired
    private ZenodoDepositorConfig config;

    @Autowired
    private BagItROService bagItROService;

    public ZenodoDepositor() { }

    public URI deposit(ResearchObject researchObject) throws DepositionException {
        try {
            ZenodoClient client = new ZenodoClient(config.getApiUrl(), config.getAccessToken());
            JsonNode depositionResponse = client.createDeposition(buildMetadata(researchObject));

            File tempFile = File.createTempFile("zenodo-payload", ".zip");
            FileOutputStream os = new FileOutputStream(tempFile);
            bagItROService.bagToZip(researchObject, os);
            JsonNode depositionFileResponse = client.createDepositionFile(tempFile,
                    depositionResponse.get("id").asInt(),
                    researchObject.getFriendlyId() + ".zip");

            return new URI(depositionFileResponse.get("links").get("self").asText());
        } catch (Exception e) {
            throw new DepositionException(e);
        }
    }

    private String buildMetadata(ResearchObject researchObject) throws Exception {
        Metadata metadata = new Metadata(Metadata.UploadType.DATASET,
                new Date(),
                researchObject.getFriendlyId(),
                "new ro",
                "1",
                Metadata.AccessRight.CLOSED);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        String string = "{\"metadata\": " + mapper.writeValueAsString(metadata) + "}";

        return string;
    }
}
