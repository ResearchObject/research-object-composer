package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.validator.ProfileValidationException;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ResearchObjectTest {
    private static ResearchObjectProfile draftTaskProfile;
    private static ResearchObjectProfile dataBundleProfile;
    private static JsonNode draftTaskContent;
    private static JsonNode dataBundleContent;

    @Before
    public void setUp() throws IOException {
        draftTaskProfile = new ResearchObjectProfile("draft_task", "/schemas/draft_task_schema.json");
        dataBundleProfile = new ResearchObjectProfile("data_bundle", "/schemas/data_bundle_schema.json");

        ObjectMapper mapper = new ObjectMapper();
        draftTaskContent = mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/draft_task_content.json"));
        dataBundleContent = mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/data_bundle_content.json"));
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

        ro.setField("workflow", draftTaskContent.get("workflow"));
        ro.setField("input", draftTaskContent.get("input"));
        ro.setField("workflow_params", draftTaskContent.get("workflow_params"));

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

        ro.appendToField("input", draftTaskContent.get("input").get(0));

        assertEquals("[\"ark://abc.123\"]", ro.getField("input").toString());
    }

    @Test
    public void appendToComplexField() {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        assertTrue(ro.supportsAppend("data"));

        assertEquals("[]", ro.getField("data").toString());

        ro.appendToField("data", dataBundleContent.get(0));

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
        ro.appendToField("data", dataBundleContent.get(0));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataBundlePatch = mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/data_bundle_patch.json"));
        ro.patchContent(dataBundlePatch);

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
            ObjectNode missingSha512 = (ObjectNode) dataBundleContent.get(0);
            missingSha512.remove("sha512");
            ro.appendToField("data", missingSha512);
            fail("RO validation should fail due to missing SHA-512 checksum");
        } catch (ProfileValidationException e) {
            JsonNode errorReport = e.toJsonNode();
            assertEquals("required key [sha512] not found", errorReport.get("message").asText());
        }
    }

    @Test
    public void validatesWhenSettingField() {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        // SHA-512
        ro.setField("data", dataBundleContent);

        try {
            // Missing SHA-512
            ((ObjectNode) dataBundleContent.get(0)).remove("sha512");
            ro.setField("data", dataBundleContent);
            fail("RO validation should fail due to missing SHA-512 checksum");
        } catch (ProfileValidationException e) {
            JsonNode errorReport = e.toJsonNode();
            assertEquals("required key [sha512] not found", errorReport.get("message").asText());
        }
    }

    @Test
    public void validatesWhenPatching() throws Exception {
        ResearchObject ro = new ResearchObject(dataBundleProfile);

        try {
            // Missing SHA-512
            ObjectMapper mapper = new ObjectMapper();
            JsonNode dataBundleBadPatch = mapper.readTree(
                    getClass().getClassLoader().getResourceAsStream("researchobject/data_bundle_invalid_patch.json"));
            ro.patchContent(dataBundleBadPatch);
            fail("RO validation should fail due to missing SHA-512 checksum");
        } catch (ProfileValidationException e) {
            JsonNode errorReport = e.toJsonNode();
            assertEquals("required key [sha512] not found", errorReport.get("message").asText());
        }
    }
}
