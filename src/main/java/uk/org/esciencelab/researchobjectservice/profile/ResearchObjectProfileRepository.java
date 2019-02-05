package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResearchObjectProfileRepository extends PagingAndSortingRepository<ResearchObjectProfile, Long> {
    Optional<ResearchObjectProfile> findByName(@Param("name") String name);
}
