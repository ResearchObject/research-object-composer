package uk.org.esciencelab.researchobjectservice.profile;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

public class ResearchObjectProfileTest {

    public static JSONObject draftTaskSchema;

    @Before
    public void setUp() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("static/draft_task_schema.json");
        draftTaskSchema = new JSONObject(new JSONTokener(is));
    }

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
