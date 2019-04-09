package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
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

    public JsonNode createDeposition(String metadata) throws Exception {
        Response response = Request.Post(depositionUrl())
                .addHeader("User-Agent", USER_AGENT)
                .bodyString(metadata, ContentType.APPLICATION_JSON).execute();
        Content content = response.returnContent();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(content.asString());

        return json;
    }

    public JsonNode createDeposition() throws Exception {
        return createDeposition("{}");
    }

    public JsonNode createDepositionFile(File file, int depositionId, String filename) throws Exception {
        HttpEntity entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setCharset(Charset.forName("UTF-8"))
                .addBinaryBody("file", file, ContentType.create("application/zip"), filename)
                .addTextBody("name", filename)
                .build();

        Response response = Request.Post(depositionFileUrl(depositionId))
                .addHeader("User-Agent", USER_AGENT)
                .body(entity).execute();

        HttpResponse r = response.returnResponse();
        StringWriter writer = new StringWriter();
        IOUtils.copy(r.getEntity().getContent(), writer, Charset.forName("UTF-8"));
        String theString = writer.toString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(theString);

        return json;
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
