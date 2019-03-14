package uk.org.esciencelab.researchobjectservice.researchobject;

/**
 * Exception thrown when client tries to query a non-existent field on a RO.
 */
public class FieldNotFoundException extends RuntimeException {
    public FieldNotFoundException(String field) {
        super(field + " is not a valid field.");
    }
}
