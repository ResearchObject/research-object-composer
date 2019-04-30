package uk.org.esciencelab.researchobjectservice.deposition;

import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.net.URI;

/**
 * Interface for depositors of research objects into external repositories.
 */
public interface Depositor {
    /**
     * Deposit the RO into the repo.
     * @param researchObject the RO to deposit.
     * @return The URI of the deposited RO within the repository.
     * @throws DepositionException
     */
    public URI deposit(ResearchObject researchObject) throws DepositionException;
}
