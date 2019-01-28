package uk.org.esciencelab.researchobjectservice;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;

import java.io.InputStream;

@SpringBootApplication
public class ResearchObjectServiceApplication implements CommandLineRunner {
    @Autowired
    private ResearchObjectProfileRepository profileRepository;

    public static void main(String[] args) {
        SpringApplication.run(ResearchObjectServiceApplication.class, args);
    }

    public void run(String... args) throws Exception {
        profileRepository.deleteAll();

        InputStream is = getClass().getClassLoader().getResourceAsStream("static/draft_task_schema.json");
        JSONObject draftTaskSchema = new JSONObject(new JSONTokener(is));
        profileRepository.save(new ResearchObjectProfile("draft_task", draftTaskSchema));
    }
}
