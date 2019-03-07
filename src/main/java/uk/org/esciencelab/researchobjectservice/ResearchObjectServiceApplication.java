package uk.org.esciencelab.researchobjectservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;

import java.io.File;

@SpringBootApplication
public class ResearchObjectServiceApplication implements CommandLineRunner {
    @Autowired
    private ResearchObjectProfileRepository profileRepository;

    public static void main(String[] args) {
        SpringApplication.run(ResearchObjectServiceApplication.class, args);
    }

    public void run(String... args) {
        // Discover schemas in resources/public/schemas and create RO profiles for each if they don't already exist.
        String schemaDir = getClass().getClassLoader().getResource("public/schemas").getPath();
        for (File schema : new File(schemaDir).listFiles()) {
            String filename = schema.getName();
            // Skip schemas whose filename begins with _ (underscore), or are not .schema.json files
            if (filename.endsWith(".schema.json") && filename.charAt(0) != '_') {
                String schemaName = filename.split("\\.")[0];
                if (!profileRepository.findByName(schemaName).isPresent()) {
                    System.out.println("Creating profile for: " + schemaName);
                    profileRepository.save(new ResearchObjectProfile(schemaName, ("/schemas/" + filename)));
                }
            }
        }
    }
}
