package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * A repository to handle fetching of RO profiles from the database.
 */
public interface ResearchObjectProfileRepository extends PagingAndSortingRepository<ResearchObjectProfile, Long> {
    /**
     * Lookup a profile by its name.
     * @param name The profile name.
     * @return
     */
    Optional<ResearchObjectProfile> findByName(@Param("name") String name);
}
