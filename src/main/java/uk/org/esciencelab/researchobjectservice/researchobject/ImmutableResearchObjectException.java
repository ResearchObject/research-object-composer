package uk.org.esciencelab.researchobjectservice.researchobject;

/**
 * Exception thrown when client tries to modify an immutable Research Object.
 */
public class ImmutableResearchObjectException extends RuntimeException {
    public ImmutableResearchObjectException() {
        super("Research Object has been deposited and cannot be modified.");
    }
}
