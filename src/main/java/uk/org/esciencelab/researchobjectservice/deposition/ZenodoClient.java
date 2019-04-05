package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;

public class ZenodoClient {
    private String baseUrl;
    private String accessToken;

    private static final String DEPOSITIONS_BASE = "/api/deposit/depositions";
    private final String USER_AGENT = "Java/Research Object Composer";

    public ZenodoClient(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.accessToken = accessToken;
    }

    public JsonNode createDeposition() throws Exception {
        Response response = Request.Post(depositionUrl())
                .addHeader("User-Agent", USER_AGENT)
                .bodyString("{}", ContentType.APPLICATION_JSON).execute();

        Content content = response.returnContent();

        content.asString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(content.asString());

        return json;
    }

    public JsonNode createDepositionFile(File file, int depositionId, String filename) throws Exception {
        Response response = Request.Post(depositionFileUrl(depositionId))
                .addHeader("User-Agent", USER_AGENT)
                .bodyFile(file, ContentType.create("application/zip"))
                .bodyForm(new BasicNameValuePair("filename", filename)).execute();

        Content content = response.returnContent();

        content.asString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(content.asString());

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
