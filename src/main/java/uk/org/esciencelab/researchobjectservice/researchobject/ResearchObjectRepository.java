package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.repository.PagingAndSortingRepository;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.util.List;

/**
 * A repository to handle fetching of research objects from the database.
 */
public interface ResearchObjectRepository extends PagingAndSortingRepository<ResearchObject, Long> {
    /**
     * Get a list of research objects that conform to the given profile.
     * @param profile
     * @return
     */
    List<ResearchObject> findAllByProfile(ResearchObjectProfile profile);
}
