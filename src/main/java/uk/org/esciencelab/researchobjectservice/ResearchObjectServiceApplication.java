package uk.org.esciencelab.researchobjectservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.esciencelab.researchobjectservice.profile.Field;
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
		Field[] fields1 = {
				new Field("fish", "java.lang.String"),
				new Field("banana", "java.net.URI")
		};
		Field[] fields2 = {
				new Field("a", "java.lang.String"),
				new Field("b", "java.lang.Integer")
		};
		profileRepository.save(new ResearchObjectProfile("finn_profile1", fields1));
		profileRepository.save(new ResearchObjectProfile("finn_profile2", fields2));
	}
}
