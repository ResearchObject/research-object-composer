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

/**
 * A very basic client to interact with Zenodo's REST API.
 * Only a very limited set of operations are supported.
 */
public class ZenodoClient {
    private String baseUrl;
    private String accessToken;

    private static final String DEPOSITIONS_BASE = "/api/deposit/depositions";
    private static final String USER_AGENT = "Java/Research Object Composer";

    /**
     * @param baseUrl The URL of the instance of Zenodo to interact with.
     *                Usually `https://zenodo.org` for production, or `https://sandbox.zenodo.org` for development.
     * @param accessToken A valid OAuth access token. We are assuming all authentication has been done externally.
     */
    public ZenodoClient(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.accessToken = accessToken;
    }

    /**
     * Create a Zenodo Deposition.
     * @param metadata A JSON document containing required metadata (see: https://developers.zenodo.org/#representation )
     * @return A Zenodo deposition resource, as JSON.
     * @throws IOException
     */
    public JsonNode createDeposition(JsonNode metadata) throws IOException {
        Request req = Request.Post(depositionUrl())
                .bodyString(metadata.toString(), ContentType.APPLICATION_JSON);

        return performRequest(req);
    }

    /**
     * Create a Zenodo Deposition with blank metadata.
     * @return A Zenodo Deposition resource, as JSON.
     * @throws IOException
     */
    public JsonNode createDeposition() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return createDeposition(mapper.createObjectNode());
    }

    /**
     * Create a file within an existing Deposition.
     * @param file The File to upload.
     * @param depositionId The ID of the Deposition to upload to.
     * @param filename The filename to use.
     * @return A Zenodo DepositionFile resource, as JSON.
     * @throws IOException
     */
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

    /**
     * Publish the specified Deposition, so it can be publicly accessed.
     * @param depositionId The ID of the Deposition to publish.
     * @return The published Zenodo deposition resource, as JSON.
     * @throws IOException
     */
    public JsonNode publishDeposition(int depositionId) throws IOException {
        Request req = Request.Post(publishDepositionUrl(depositionId));

        return performRequest(req);
    }

    private JsonNode performRequest(Request request) throws IOException, DepositionException {
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

    private String publishDepositionUrl(int depositionId) {
        return buildUrl(DEPOSITIONS_BASE + "/" + depositionId + "/actions/publish");
    }

    private String buildUrl(String path) {
        return this.baseUrl + path + "?access_token=" + this.accessToken;
    }
}
