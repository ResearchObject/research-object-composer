package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResearchObjectProfileTest {
    private static final String draftTaskSchema = "schemas/draft_task_schema.json";

    @Test
    public void createProfile() {
        ResearchObjectProfile profile = new ResearchObjectProfile("draft_task", draftTaskSchema);

        assertEquals("draft_task", profile.getName());
        assertArrayEquals(new String [] {"input", "workflow", "workflow_params"}, profile.getFields());
        assertTrue(profile.hasField("input"));
        assertFalse(profile.hasField("banana"));
    }

    @Test
    public void getProfileTemplate() {
        ResearchObjectProfile profile = new ResearchObjectProfile("draft_task", draftTaskSchema);
        JsonNode template = profile.getTemplate();

        assertEquals(3, template.size());
        assertEquals("[]", template.get("input").toString());
        assertEquals("null", template.get("workflow").toString());
        assertEquals("{}", template.get("workflow_params").toString());
    }
}
