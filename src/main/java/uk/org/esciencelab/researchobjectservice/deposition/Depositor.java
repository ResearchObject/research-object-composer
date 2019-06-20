package uk.org.esciencelab.researchobjectservice.deposition;

import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.net.URI;
import java.util.Map;

/**
 * Interface for depositors of Research Objects into external repositories.
 */
public interface Depositor {
    /**
     * Deposit the RO into the repo.
     * @param researchObject the RO to deposit.
     * @param params optional parameters to pass through to the depositor.
     * @return The URI of the deposited RO within the repository.
     * @throws DepositionException
     */
    public URI deposit(ResearchObject researchObject, Map<String, String> params) throws DepositionException;
}
