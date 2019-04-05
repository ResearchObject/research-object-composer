package uk.org.esciencelab.researchobjectservice.deposition;

public class DepositionException extends RuntimeException {

    /**
     * @param e The original Exception thrown when trying to deposit.
     */
    public DepositionException(Exception e) {
        super(e);
    }
}
