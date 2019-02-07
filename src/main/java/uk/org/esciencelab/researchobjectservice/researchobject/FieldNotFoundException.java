package uk.org.esciencelab.researchobjectservice.researchobject;

public class FieldNotFoundException extends RuntimeException {
    public FieldNotFoundException(String field) {
        super(field + " is not a valid field.");
    }
}
