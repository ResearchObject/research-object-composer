package uk.org.esciencelab.researchobjectservice.profile;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResearchObjectProfileTest {

    private static final String draftTaskSchema = "schemas/draft_task_schema.json";

    @Test
    public void createProfile() {
        ResearchObjectProfile profile = new ResearchObjectProfile("draft_task", draftTaskSchema);

        assertEquals("draft_task", profile.getId());
        assertArrayEquals(new String [] {"input", "workflow", "workflow_params"}, profile.getFields());
        assertTrue(profile.hasField("input"));
        assertFalse(profile.hasField("banana"));
        assertEquals("org.everit.json.schema.ArraySchema", profile.getFieldSchema("input").getClass().getName());
        assertEquals("org.everit.json.schema.StringSchema", profile.getFieldSchema("workflow").getClass().getName());
        assertEquals("org.everit.json.schema.ObjectSchema", profile.getFieldSchema("workflow_params").getClass().getName());
    }

    @Test
    public void getProfileTemplate() {
        ResearchObjectProfile profile = new ResearchObjectProfile("draft_task", draftTaskSchema);
        JSONObject template = profile.getTemplate();

        assertEquals(3, template.keySet().size());
        assertEquals("[]", template.get("input").toString());
        assertEquals("null", template.get("workflow").toString());
        assertEquals("{}", template.get("workflow_params").toString());
    }
}
