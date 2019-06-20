package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

/**
 * A repository to handle fetching of Research Objects from the database.
 */
public interface ResearchObjectRepository extends PagingAndSortingRepository<ResearchObject, Long> {
    /**
     * Get a list of Research Objects that conform to the given profile.
     * @param profile
     * @return
     */
    Page<ResearchObject> findAllByProfile(Pageable pageable, ResearchObjectProfile profile);
}
