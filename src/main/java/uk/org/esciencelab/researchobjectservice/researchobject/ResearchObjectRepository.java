package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.util.List;

public interface ResearchObjectRepository extends MongoRepository<ResearchObject, String> {
    List<ResearchObject> findAllByProfile(ResearchObjectProfile profile);
}
