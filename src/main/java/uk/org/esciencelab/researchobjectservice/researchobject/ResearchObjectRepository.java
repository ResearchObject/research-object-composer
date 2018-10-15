package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResearchObjectRepository extends MongoRepository<ResearchObject, String> {
    List<ResearchObject> findAllByProfileId(String profileId);
}
