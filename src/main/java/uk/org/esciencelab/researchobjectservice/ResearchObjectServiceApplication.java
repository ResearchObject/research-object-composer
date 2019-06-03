package uk.org.esciencelab.researchobjectservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A web service to facilitate the creation of Research Objects, constrained to pre-defined profiles.
 */
@SpringBootApplication
public class ResearchObjectServiceApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ResearchObjectServiceApplication.class);

    @Autowired
    private ResearchObjectProfileRepository profileRepository;

    public static void main(String[] args) {
        SpringApplication.run(ResearchObjectServiceApplication.class, args);
    }

    public void run(String... args) {
        // Discover schemas in resources/public/schemas and create RO profiles for each if they don't already exist.
        logger.info("Discovering schemas...");
        String schemaDir = getClass().getClassLoader().getResource("public/schemas").getPath();
        discoverSchemas(new File(schemaDir), Paths.get("/"));

        // Delete any profiles for schema files that no longer exist
        for (ResearchObjectProfile profile : profileRepository.findAll()) {
            if (getClass().getClassLoader().getResource("public" + profile.getSchemaPath()) == null) {
                logger.info("Schema for profile: " + profile.getName() + " no longer exists, deleting...");
                profileRepository.delete(profile);
            }
        }
    }

    private void discoverSchemas(File directory, Path path) {
        for (File entry : directory.listFiles()) {
            String name = entry.getName();
            if (entry.isDirectory()) {
                discoverSchemas(entry, path.resolve(name));
            } else {
                // Skip schemas whose filename begins with _ (underscore), or are not .schema.json files
                if (name.endsWith(".schema.json") && name.charAt(0) != '_') {
                    String schemaName = path.resolve(name.split("\\.")[0])
                            .toString()
                            .substring(1)
                            .replaceAll("[^a-zA-Z0-9_]", "_");
                    String schemaPath = "/schemas" + path.resolve(name).toString();
                    logger.info("Found schema: " + schemaName + " (in public" + schemaPath + ")");
                    if (!profileRepository.findByName(schemaName).isPresent()) {
                        logger.info("Creating ResearchObjectProfile for: " + schemaName);
                        profileRepository.save(new ResearchObjectProfile(schemaName, schemaPath));
                    }
                }
            }
        }
    }
}
