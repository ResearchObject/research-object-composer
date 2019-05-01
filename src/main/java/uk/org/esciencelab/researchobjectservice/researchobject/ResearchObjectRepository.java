package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.repository.PagingAndSortingRepository;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.util.List;

/**
 * A repository to handle fetching of Research Objects from the database.
 */
public interface ResearchObjectRepository extends PagingAndSortingRepository<ResearchObject, Long> {
    /**
     * Get a list of Research Objects that conform to the given profile.
     * @param profile
     * @return
     */
    List<ResearchObject> findAllByProfile(ResearchObjectProfile profile);
}
