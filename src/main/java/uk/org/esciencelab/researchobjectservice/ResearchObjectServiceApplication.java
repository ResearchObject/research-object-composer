package uk.org.esciencelab.researchobjectservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;

@SpringBootApplication
public class ResearchObjectServiceApplication implements CommandLineRunner {
    @Autowired
    private ResearchObjectProfileRepository profileRepository;

    public static void main(String[] args) {
        SpringApplication.run(ResearchObjectServiceApplication.class, args);
    }

    public void run(String... args) throws Exception {
        profileRepository.deleteAll();

        profileRepository.save(new ResearchObjectProfile("draft_task", "static/draft_task_schema.json"));
        profileRepository.save(new ResearchObjectProfile("data_bundle", "static/data_bundle_schema.json"));
    }
}
