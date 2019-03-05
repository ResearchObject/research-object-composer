package uk.org.esciencelab.researchobjectservice.bagit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
public class ResearchObjectBaggerServiceTest {

    private ResearchObjectBaggerService researchObjectBaggerService;
    private static ResearchObjectProfile draftTaskProfile;
    private static ResearchObjectProfile dataBundleProfile;
    private static ResearchObject draftTaskRO;

    @Before
    public void setUp() throws IOException {
        draftTaskProfile = new ResearchObjectProfile("draft_task", "/schemas/draft_task_schema.json");
        dataBundleProfile = new ResearchObjectProfile("data_bundle", "/schemas/data_bundle_schema.json");

        ObjectMapper mapper = new ObjectMapper();
        draftTaskRO = new ResearchObject(draftTaskProfile);
        draftTaskRO.setContent((ObjectNode) mapper.readTree(getClass().getClassLoader().getResourceAsStream("researchobject/draft_task_content.json")));

        researchObjectBaggerService = new ResearchObjectBaggerService();
    }

    @Test
    public void gatherBaggableEntriesForDraftTask() throws IOException {
        Path bagLocation = Files.createTempDirectory("bag");

        ArrayList<BagEntry> entries = researchObjectBaggerService.gatherBagEntries(new ArrayList<>(), bagLocation,
                draftTaskRO.getContent(),
                draftTaskRO.getProfile().getSchemaWrapper().getObjectSchema(), null);

        System.out.println(entries);
    }
}
