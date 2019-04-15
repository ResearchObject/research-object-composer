package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

public class ZenodoClient {
    private String baseUrl;
    private String accessToken;

    private static final String DEPOSITIONS_BASE = "/api/deposit/depositions";
    private final String USER_AGENT = "Java/Research Object Composer";

    public ZenodoClient(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.accessToken = accessToken;
    }

    public JsonNode createDeposition(JsonNode metadata) throws IOException {
        Request req = Request.Post(depositionUrl())
                .bodyString(metadata.toString(), ContentType.APPLICATION_JSON);

        return performRequest(req);
    }

    public JsonNode createDeposition() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return createDeposition(mapper.createObjectNode());
    }

    public JsonNode createDepositionFile(File file, int depositionId, String filename) throws IOException {
        HttpEntity entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setCharset(Charset.forName("UTF-8"))
                .addBinaryBody("file", file, ContentType.create("application/zip"), filename)
                .addTextBody("name", filename)
                .build();

        Request req = Request.Post(depositionFileUrl(depositionId))
                .body(entity);

        return performRequest(req);
    }

    private JsonNode performRequest(Request request) throws IOException {
        Response response = request
                .addHeader("User-Agent", USER_AGENT)
                .execute();

        HttpResponse r = response.returnResponse();
        StringWriter writer = new StringWriter();
        IOUtils.copy(r.getEntity().getContent(), writer, Charset.forName("UTF-8"));
        ObjectMapper mapper = new ObjectMapper();

        if (r.getStatusLine().getStatusCode() >= 400) {
            JsonNode node = mapper.readTree(writer.toString());
            throw new DepositionException(r.getStatusLine().getStatusCode(), node);
        }

        return mapper.readTree(writer.toString());
    }

    private String depositionUrl() {
        return buildUrl(DEPOSITIONS_BASE);
    }

    private String depositionFileUrl(int depositionId) {
        return buildUrl(DEPOSITIONS_BASE + "/" + depositionId + "/files");
    }

    private String buildUrl(String path) {
        return this.baseUrl + path + "?access_token=" + this.accessToken;
    }
}
