package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class BagEntryTest {

    @BeforeClass
    public static void init() {
        org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.getInstance();
    }

    private static BagEntry inputEntry;
    private static BagEntry workflowEntry;

    @Before
    public void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode draftTaskContent = mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/draft_task_content.json"));
        JsonNode input = draftTaskContent.get("input").get(0);
        inputEntry = new BagEntry("/input", input);
        JsonNode workflow = draftTaskContent.get("workflow");
        workflowEntry = new BagEntry("/workflows/deep/folder", workflow);
    }

    @Test
    public void testGetters() {
        assertEquals("a.xml", inputEntry.getFilename());
        assertEquals("/data/input/a.xml", inputEntry.getFilepath().toString());
        assertEquals(null, inputEntry.getChecksum("MD5"));
        assertEquals("87428fc522803d31065e7bce3cf03fe475096631e5e07bbd7a0fde60c4cf25c7", inputEntry.getChecksum("SHA-256"));

        assertEquals("workflow.cwl", workflowEntry.getFilename());
        assertEquals("/data/workflows/deep/folder/workflow.cwl", workflowEntry.getFilepath().toString());
        assertEquals("df3e129a722a865cc3539b4e69507bad", workflowEntry.getChecksum("MD5"));
        assertEquals(null, workflowEntry.getChecksum("SHA-256"));
        assertEquals(null, workflowEntry.getChecksum("SHA-224"));
        assertEquals(null, workflowEntry.getChecksum("SHA-512"));
    }

    @Test
    public void testPathMetadata() {
        PathMetadata inputMeta = inputEntry.getPathMetadata();
        assertEquals("https://www.example.com/data/a", inputMeta.getUri().toString());
        assertEquals("../data/input", inputMeta.getBundledAs().getFolder().toString());
        assertEquals("a.xml", inputMeta.getBundledAs().getFilename());

        PathMetadata workflowMeta = workflowEntry.getPathMetadata();
        assertEquals("https://www.myexperiment.org/workflows/5044", workflowMeta.getUri().toString());
        assertEquals("../data/workflows/deep/folder", workflowMeta.getBundledAs().getFolder().toString());
        assertEquals("workflow.cwl", workflowMeta.getBundledAs().getFilename());
    }
}
