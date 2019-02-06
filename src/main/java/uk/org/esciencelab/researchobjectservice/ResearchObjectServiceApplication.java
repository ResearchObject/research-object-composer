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
        if (!profileRepository.findByName("draft_task").isPresent()) {
            profileRepository.save(new ResearchObjectProfile("draft_task", "schemas/draft_task_schema.json"));
        }

        if (!profileRepository.findByName("data_bundle").isPresent()) {
            profileRepository.save(new ResearchObjectProfile("data_bundle", "schemas/data_bundle_schema.json"));
        }
    }
}
