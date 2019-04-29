package uk.org.esciencelab.researchobjectservice.researchobject;

/**
 * Exception thrown when client tries to modify an immutable research object.
 */
public class ImmutableResearchObjectException extends RuntimeException {
    public ImmutableResearchObjectException() {
        super("Research Object has been deposited and cannot be modified.");
    }
}
