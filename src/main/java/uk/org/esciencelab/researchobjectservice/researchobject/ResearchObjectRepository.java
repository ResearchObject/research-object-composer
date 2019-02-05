package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.repository.PagingAndSortingRepository;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.util.List;

public interface ResearchObjectRepository extends PagingAndSortingRepository<ResearchObject, Long> {
    List<ResearchObject> findAllByProfile(ResearchObjectProfile profile);
}
