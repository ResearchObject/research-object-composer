package uk.org.esciencelab.researchobjectservice.researchobject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResearchObjectTest {

    public static ResearchObjectProfile draftTaskProfile;
    public static ResearchObjectProfile dataBundleProfile;

    @Before
    public void setUp() {
        draftTaskProfile = new ResearchObjectProfile("draft_task", "static/draft_task_schema.json");
        dataBundleProfile = new ResearchObjectProfile("data_bundle", "static/data_bundle_schema.json");
    }

    @Test
    public void createRO() {
        ResearchObject ro = new ResearchObject(draftTaskProfile);

        assertEquals("draft_task", ro.getProfile().getId());

        JSONObject fields = ro.getFields();
        assertEquals(3, fields.keySet().size());
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

        JSONObject fields = ro.getFields();
        JSONArray input = (JSONArray) fields.get("input");
        assertEquals(2, input.length());
        assertArrayEquals(new String [] {"ark://abc.123", "ark://abc.456"}, input.toList().toArray());
        assertEquals("ark://xyz.123", fields.get("workflow").toString());
        assertEquals(123, ((JSONObject) fields.get("workflow_params")).get("x"));

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

        ro.appendToField("data","{\"length\": 123,\"filename\": \"important_doc.pdf\",\"sha256\": \"5a81483d96b0bc15ad19af7f5a662e14b275729fbc05579b18513e7f550016b1\",\"url\" : \"http://example.com/important_doc.pdf\"}");

        JSONArray data = (JSONArray) ro.getField("data");
        assertEquals(1, data.length());
        JSONObject importantDoc = (JSONObject) data.get(0);
        assertEquals("important_doc.pdf", importantDoc.get("filename"));
        assertEquals("5a81483d96b0bc15ad19af7f5a662e14b275729fbc05579b18513e7f550016b1", importantDoc.get("sha256"));
        assertEquals(123, importantDoc.getInt("length"));

    }


}
