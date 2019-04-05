package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;
import uk.org.esciencelab.researchobjectservice.serialization.BagItROService;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

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
            JsonNode depositionResponse = client.createDeposition();
            System.out.println(depositionResponse);

            File tempFile = File.createTempFile("zenodo-payload", ".zip");
            FileOutputStream os = new FileOutputStream(tempFile);
            bagItROService.bagToZip(researchObject, os);
            JsonNode depositionFileResponse = client.createDepositionFile(tempFile,
                    depositionResponse.get("id").asInt(),
                    researchObject.getFriendlyId() + ".zip");
            System.out.println(depositionFileResponse);

            return new URI(depositionFileResponse.get("links").get("self").asText());
        } catch (Exception e) {
            throw new DepositionException(e);
        }
    }
}
