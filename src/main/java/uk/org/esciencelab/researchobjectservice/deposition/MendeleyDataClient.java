package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * A very basic client to interact with Mendeley Data's REST API.
 * Only a very limited set of operations are supported.
 */
public class MendeleyDataClient {
    private String baseUrl;
    private String accessToken;

    private static final String USER_AGENT = "Java/Research Object Composer";
    private static final String DRAFT_DATASET_TYPE = "application/vnd.mendeley-draft-dataset.1+json";
    private static final String DRAFT_DATASET_CREATE_TYPE = "application/vnd.mendeley-dataset-creation-request.1+json";
    private static final String DRAFT_DATASET_PATCH_TYPE = "application/vnd.mendeley-dataset-patch.1+json";
    private static final String FILE_CONTENT_TYPE = "application/vnd.mendeley-content-ticket.1+json";

    public MendeleyDataClient(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.accessToken = accessToken;
    }

    /**
     * Create a Mendeley Data Dataset.
     * @param metadata A JSON document containing required metadata (see: https://dev.mendeley.com/methods/#public-dataset-attributes)
     * @return A Mendeley Data Dataset resource, as JSON.
     * @throws IOException
     */
    public JsonNode createDataset(JsonNode metadata) throws IOException {
        Request req = Request.Post(buildUrl("/datasets/drafts"))
                .addHeader("Accept", "application/vnd.mendeley-draft-dataset.1+json")
                .bodyString(metadata.toString(), ContentType.create("application/vnd.mendeley-dataset-creation-request.1+json"));

        return performRequest(req);
    }

    /**
     * Create a Mendeley Data File Content resource.
     * @param file The File to upload.
     * @return A Mendeley Data File Content resource, as JSON.
     * @throws IOException
     */
    public JsonNode createFileContent(File file) throws IOException {
        Request req = Request.Post(buildUrl("/file_contents"))
                .addHeader("Accept", FILE_CONTENT_TYPE)
                .bodyFile(file, ContentType.create("application/zip"));

        return performRequest(req);
    }

    /**
     * Add a created file to an existing Mendeley Data Dataset.
     * @param datasetId The ID of the Dataset.
     * @param fileId The ID of the File Content.
     * @param filename The filename for the file.
     * @param description A description of how the file relates to the dataset.
     * @return The modified Mendeley Data Dataset resource, as JSON.
     * @throws IOException
     */
    public JsonNode addFileToDataset(String datasetId, String fileId, String filename, String description) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode content = mapper.createObjectNode();
        content.put("filename", filename);
        content.put("description", description);
        ObjectNode contentDetails = mapper.createObjectNode();
        contentDetails.put("id", fileId);
        content.set("content_details", contentDetails);

        Request req = Request.Patch(buildUrl("/datasets/drafts/" + datasetId))
                .addHeader("Accept", DRAFT_DATASET_TYPE)
                .bodyString(content.toString(), ContentType.create(DRAFT_DATASET_PATCH_TYPE));

        return performRequest(req);
    }

    private JsonNode performRequest(Request request) throws IOException, DepositionException {
        Response response = request
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Authorization", "Bearer " + accessToken)
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

    /**
     * Publish a Mendeley Data draft Dataset.
     * @param datasetId The ID of the Dataset.
     * @return The modified Mendeley Data Dataset resource, as JSON.
     * @throws IOException
     */
    public JsonNode publishDataset(String datasetId) throws IOException {
        Request req = Request.Post(buildUrl("/datasets/drafts/" + datasetId))
                .addHeader("Accept", DRAFT_DATASET_TYPE)
                .addHeader("Content-Type", DRAFT_DATASET_TYPE);

        return performRequest(req);
    }

    private String buildUrl(String path) {
        return this.baseUrl + path;
    }
}
