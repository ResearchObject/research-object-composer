package uk.org.esciencelab.researchobjectservice.bagit;

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

public class ResearchObjectBaggerServiceTest {

    @BeforeClass
    public static void init() {
        org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.getInstance();
    }

    private ResearchObjectBaggerService researchObjectBaggerService;
    private static ResearchObjectProfile draftTaskProfile;
    private static ResearchObjectProfile dataBundleProfile;
    private static ResearchObjectProfile complexProfile;

    @Before
    public void setUp() throws IOException {
        draftTaskProfile = new ResearchObjectProfile("draft_task", "/schemas/draft_task.schema.json");
        dataBundleProfile = new ResearchObjectProfile("data_bundle", "/schemas/data_bundle.schema.json");
        complexProfile = new ResearchObjectProfile("complex", "/schemas/complex.schema.json");

        researchObjectBaggerService = new ResearchObjectBaggerService();
    }

    @Test
    public void gatherBaggableEntriesForDraftTask() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ResearchObject draftTaskRO = new ResearchObject(draftTaskProfile);
        draftTaskRO.setContent((ObjectNode) mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/draft_task_content.json")));

        Path bagLocation = Files.createTempDirectory("test_bag");

        ArrayList<BagEntry> entries = new ArrayList<>();
        researchObjectBaggerService.gatherBagEntries(entries, bagLocation, draftTaskRO.getContent(),
                draftTaskRO.getProfile().getSchemaWrapper().getObjectSchema(), null);

//        [BagEntry: (workflow.cwl @ https://www.myexperiment.org/workflows/5044 (1000) /tmp/test_bag6900967083638881976/data/workflow]
//        [BagEntry: (a.xml @ https://www.example.com/data/a (999) /tmp/test_bag6900967083638881976/data/input]
//        [BagEntry: (b.json @ https://www.example.com/data/b (888) /tmp/test_bag6900967083638881976/data/input]

        assertEquals(3, entries.size());
    }

    @Test
    public void gatherBaggableEntriesForComplexRO() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ResearchObject complexRO = new ResearchObject(complexProfile);
        complexRO.setContent((ObjectNode) mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/complex_content.json")));

        Path bagLocation = Files.createTempDirectory("test_bag");

        ArrayList<BagEntry> entries = new ArrayList<>();
        researchObjectBaggerService.gatherBagEntries(entries, bagLocation, complexRO.getContent(),
                complexRO.getProfile().getSchemaWrapper().getObjectSchema(), null);
//[BagEntry: (usage_policy.docx @ https://www.example.com/project/docs/usage_policy.docx (999) /tmp/test_bag7873253758624771240/data/policy]
//[BagEntry: (coordinates.csv @ https://www.example.com/project/data/coordinates.csv (634634) /tmp/test_bag7873253758624771240/data/shared_data]
//[BagEntry: (sop.txt @ https://www.example.com/project/docs/sop.txt (351141) /tmp/test_bag7873253758624771240/data/shared_data]
//[BagEntry: (alpha_final.csv @ https://www.example.com/project/data/alpha/alpha_final.csv (975371) /tmp/test_bag7873253758624771240/data/group_data]
//[BagEntry: (data.zip @ https://www.example.com/project/data/alpha/data.zip (78416) /tmp/test_bag7873253758624771240/data/group_data]
//[BagEntry: (bravo_final.csv @ https://www.example.com/project/data/bravo/bravo_final.csv (763361) /tmp/test_bag7873253758624771240/data/group_data]
//[BagEntry: (data.zip @ https://www.example.com/project/data/bravo/data.zip (11005) /tmp/test_bag7873253758624771240/data/group_data]
//[BagEntry: (readme.txt @ https://www.example.com/project/data/bravo/readme.txt (5167) /tmp/test_bag7873253758624771240/data/group_data]

        assertEquals(8, entries.size());
    }
}
