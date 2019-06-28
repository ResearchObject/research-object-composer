package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class BagItROServiceTest {

    @BeforeClass
    public static void init() {
        org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.getInstance();
    }

    private static BagItROService bagItROService;
    private static ResearchObjectProfile draftTaskProfile;
    private static ResearchObjectProfile dataBundleProfile;
    private static ResearchObjectProfile complexProfile;

    @Before
    public void setUp() {
        draftTaskProfile = new ResearchObjectProfile("draft_task", "/schemas/draft_task.schema.json");
        dataBundleProfile = new ResearchObjectProfile("data_bundle", "/schemas/data_bundle.schema.json");
        complexProfile = new ResearchObjectProfile("complex", "/schemas/complex.schema.json");

        bagItROService = new BagItROService();
    }

    @Test
    public void gatherBaggableEntriesForDraftTask() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ResearchObject draftTaskRO = new ResearchObject(draftTaskProfile);
        draftTaskRO.setContent((ObjectNode) mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/draft_task_content.json")));

        ArrayList<RemoteResource> entries = bagItROService.gatherBagEntries( draftTaskRO.getContent(),
                draftTaskRO.getProfile().getSchemaWrapper().getObjectSchema(), null);

        assertEquals(3, entries.size());

        RemoteResource workflowEntry = entries.stream()
                .filter(x -> "https://www.myexperiment.org/workflows/5044".equals(x.getUrl().toString()))
                .findFirst().get();
        assertEquals("data/workflow/workflow.cwl", workflowEntry.getFilepath().toString());
        assertEquals(1000, workflowEntry.getLength());
        assertEquals("df3e129a722a865cc3539b4e69507bad", workflowEntry.getChecksum("MD5"));
        assertEquals(null, workflowEntry.getChecksum("SHA-256"));
        assertEquals(null, workflowEntry.getChecksum("SHA-224"));
        assertEquals(null, workflowEntry.getChecksum("SHA-512"));

        RemoteResource xmlEntry = entries.stream()
                .filter(x -> "https://www.example.com/data/a".equals(x.getUrl().toString()))
                .findFirst().get();
        assertEquals("data/input/a.xml", xmlEntry.getFilepath().toString());
        assertEquals(999, xmlEntry.getLength());
        assertEquals(null, xmlEntry.getChecksum("MD5"));
        assertEquals("87428fc522803d31065e7bce3cf03fe475096631e5e07bbd7a0fde60c4cf25c7", xmlEntry.getChecksum("SHA-256"));

        RemoteResource jsonEntry = entries.stream()
                .filter(x -> "https://www.example.com/data/b".equals(x.getUrl().toString()))
                .findFirst().get();
        assertEquals("data/input/b.json", jsonEntry.getFilepath().toString());
        assertEquals(888, jsonEntry.getLength());
        assertEquals("3b5d5c3712955042212316173ccf37be", jsonEntry.getChecksum("MD5"));
        assertEquals("0263829989b6fd954f72baaf2fc64bc2e2f01d692d4de72986ea808f6e99813f", jsonEntry.getChecksum("SHA-256"));
    }

    @Test
    public void gatherBaggableEntriesForComplexRO() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ResearchObject complexRO = new ResearchObject(complexProfile);
        complexRO.setContent((ObjectNode) mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/complex_content.json")));

        ArrayList<RemoteResource> entries = bagItROService.gatherBagEntries(complexRO.getContent(),
                complexRO.getProfile().getSchemaWrapper().getObjectSchema(), null);
//[RemoteResource: (usage_policy.docx @ https://www.example.com/project/docs/usage_policy.docx (999) /tmp/test_bag7873253758624771240/data/policy]
//[RemoteResource: (coordinates.csv @ https://www.example.com/project/data/coordinates.csv (634634) /tmp/test_bag7873253758624771240/data/shared_data]
//[RemoteResource: (sop.txt @ https://www.example.com/project/docs/sop.txt (351141) /tmp/test_bag7873253758624771240/data/shared_data]
//[RemoteResource: (alpha_final.csv @ https://www.example.com/project/data/alpha/alpha_final.csv (975371) /tmp/test_bag7873253758624771240/data/group_data]
//[RemoteResource: (data.zip @ https://www.example.com/project/data/alpha/data.zip (78416) /tmp/test_bag7873253758624771240/data/group_data]
//[RemoteResource: (bravo_final.csv @ https://www.example.com/project/data/bravo/bravo_final.csv (763361) /tmp/test_bag7873253758624771240/data/group_data]
//[RemoteResource: (data.zip @ https://www.example.com/project/data/bravo/data.zip (11005) /tmp/test_bag7873253758624771240/data/group_data]
//[RemoteResource: (readme.txt @ https://www.example.com/project/data/bravo/readme.txt (5167) /tmp/test_bag7873253758624771240/data/group_data]

        assertEquals(8, entries.size());

        long groupDataCount = entries.stream()
                .filter(x -> x.getFilepath().startsWith("data/group_data")).count();
        assertEquals(5, groupDataCount);

        long sharedDataCount = entries.stream()
                .filter(x -> x.getFilepath().startsWith("data/shared_data")).count();
        assertEquals(2, sharedDataCount);
    }
}
