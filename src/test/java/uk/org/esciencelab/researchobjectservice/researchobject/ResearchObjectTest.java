package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.validator.ProfileValidationException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ResearchObjectTest {
    private static ResearchObjectProfile draftTaskProfile;
    private static ResearchObjectProfile dataBundleProfile;

    // This is needed to handle the "classpath" protocol used to join resolve $refs in the JSON schemas.
    @BeforeClass
    public static void init() {
        org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.getInstance();
    }

    @Before
    public void setUp() {
        draftTaskProfile = new ResearchObjectProfile("draft_task", "schemas/draft_task_schema.json");
        dataBundleProfile = new ResearchObjectProfile("data_bundle", "schemas/data_bundle_schema.json");
    }

    @Test
    public void createRO() {
        ResearchObject ro = new ResearchObject(draftTaskProfile);

        assertEquals("draft_task", ro.getProfile().getName());

        ObjectNode fields = ro.getContent();
        assertEquals(3, fields.size());
        assertEquals("[]", fields.get("input").toString());
        assertEquals("null", fields.get("workflow").toString());
        assertEquals("{}", fields.get("workflow_params").toString());
    }

    @Test
    public void getAndSetROFields() {
        ResearchObject ro = new ResearchObject(draftTaskProfile);

        ro.setField("workflow", "\"ark://xyz.123\"");
        ro.setField("input", "[\"ark://abc.123\", \"ark://abc.456\"]");
        ro.setField("workflow_params", "{ \"x\" : 123 }");

        ObjectNode fields = ro.getContent();
        ArrayNode input = (ArrayNode) fields.get("input");
        assertEquals(2, input.size());
        assertEquals("ark://abc.123", input.get(0).asText());
        assertEquals("ark://abc.456", input.get(1).asText());
        assertEquals("ark://xyz.123", fields.get("workflow").asText());
        assertEquals(123, fields.get("workflow_params").get("x").asInt());

        ro.clearField("workflow");
        assertEquals("null", fields.get("workflow").toString());

        ro.clearField("input");
        assertEquals("[]", fields.get("input").toString());

        ro.clearField("workflow_params");
        assertEquals("{}", fields.get("workflow_params").toString());
    }

    @Test
    public void appendToField() {
        ResearchObject ro = new ResearchObject(draftTaskProfile);

        assertTrue(ro.supportsAppend("input"));
        assertFalse(ro.supportsAppend("workflow"));
        assertFalse(ro.supportsAppend("workflow_params"));

        assertEquals("[]", ro.getField("input").toString());

        ro.appendToField("input", "\"ark://xyz.123\"");

        assertEquals("[\"ark://xyz.123\"]", ro.getField("input").toString());
    }

    @Test
    public void appendToComplexField() {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        assertTrue(ro.supportsAppend("data"));

        assertEquals("[]", ro.getField("data").toString());

        ro.appendToField("data","{\"length\": 123,\"filename\": \"important_doc.pdf\",\"sha512\": \"a131b5e2cb03fbeae9ba608b2912b27d73540a53562dcc752d43a499541e948682158c432cd1dcb55542d0fc84d9164963a8b6d7d6838f8e033cfe4449d1dd4c\",\"url\" : \"http://example.com/important_doc.pdf\"}");

        ArrayNode data = (ArrayNode) ro.getField("data");
        assertEquals(1, data.size());
        ObjectNode importantDoc = (ObjectNode) data.get(0);
        assertEquals("important_doc.pdf", importantDoc.get("filename").asText());
        assertEquals("a131b5e2cb03fbeae9ba608b2912b27d73540a53562dcc752d43a499541e948682158c432cd1dcb55542d0fc84d9164963a8b6d7d6838f8e033cfe4449d1dd4c", importantDoc.get("sha512").asText());
        assertEquals(123, importantDoc.get("length").asInt());
    }

    @Test
    public void patchContent() throws Exception {
        ResearchObject ro = new ResearchObject(dataBundleProfile);
        ro.appendToField("data","{\"length\": 123,\"filename\": \"important_doc.pdf\",\"sha512\": \"a131b5e2cb03fbeae9ba608b2912b27d73540a53562dcc752d43a499541e948682158c432cd1dcb55542d0fc84d9164963a8b6d7d6838f8e033cfe4449d1dd4c\",\"url\" : \"http://example.com/important_doc.pdf\"}");

        ro.patchContent("[" +
                "{ \"op\" : \"replace\",  \"path\": \"/data/0/filename\", \"value\" : \"very_important_doc.pdf\" }," +
                "{ \"op\" : \"add\",  \"path\": \"/data/-\", \"value\" : {\"length\": 123,\"filename\": \"another_important_doc.pdf\",\"sha512\": \"a131b5e2cb03fbeae9ba608b2912b27d73540a53562dcc752d43a499541e948682158c432cd1dcb55542d0fc84d9164963a8b6d7d6838f8e033cfe4449d1dd4c\", \"url\" : \"http://example.com/another_important_doc.pdf\"}}" +
                "]");

        ArrayNode data = (ArrayNode) ro.getField("data");
        assertEquals(2, data.size());
        ObjectNode importantDoc = (ObjectNode) data.get(0);
        ObjectNode anotherImportantDoc = (ObjectNode) data.get(1);
        assertEquals("very_important_doc.pdf", importantDoc.get("filename").asText());
        assertEquals(123, importantDoc.get("length").asInt());
        assertEquals("another_important_doc.pdf", anotherImportantDoc.get("filename").asText());
        assertEquals(123, anotherImportantDoc.get("length").asInt());
    }

    @Test
    public void validatesWhenAppendingField() {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        try {
            // Missing SHA-512
            ro.appendToField("data","{\"length\": 123,\"filename\": \"important_doc.pdf\",\"url\" : \"http://example.com/important_doc.pdf\"}");
            fail("RO validation should fail due to missing SHA-512 checksum");
        } catch (ProfileValidationException e) {
            JSONObject errorReport = e.toJSON();
            assertEquals("required key [sha512] not found", errorReport.get("message"));
        }
    }

    @Test
    public void validatesWhenSettingField() {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        // SHA-512
        ro.setField("data","[{\"length\": 123,\"filename\": \"important_doc.pdf\",\"sha512\": \"a131b5e2cb03fbeae9ba608b2912b27d73540a53562dcc752d43a499541e948682158c432cd1dcb55542d0fc84d9164963a8b6d7d6838f8e033cfe4449d1dd4c\",\"url\" : \"http://example.com/important_doc.pdf\"}]");

        try {
            // Missing SHA-512
            ro.setField("data", "[{\"length\": 123,\"filename\": \"important_doc.pdf\",\"url\" : \"http://example.com/important_doc.pdf\"}]");
            fail("RO validation should fail due to missing SHA-512 checksum");
        } catch (ProfileValidationException e) {
            JSONObject errorReport = e.toJSON();
            assertEquals("required key [sha512] not found", errorReport.get("message"));
        }
    }

    @Test
    public void validatesWhenPatching() throws Exception {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        try {
            // Missing SHA-512
            ro.patchContent("[{ \"op\" : \"add\",  \"path\": \"/data/-\", \"value\" : {\"length\": 123,\"filename\": \"important_doc.pdf\",\"url\" : \"http://example.com/important_doc.pdf\"}}]");
            fail("RO validation should fail due to missing SHA-512 checksum");
        } catch (ProfileValidationException e) {
            JSONObject errorReport = e.toJSON();
            assertEquals("required key [sha512] not found", errorReport.get("message"));
        }
    }
}
