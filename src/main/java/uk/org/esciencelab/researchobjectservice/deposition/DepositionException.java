package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception thrown when an error occurs during the deposition process.
 */
@JsonIgnoreProperties({ "cause", "stackTrace", "suppressed", "localizedMessage" })
public class DepositionException extends RuntimeException {
    private JsonNode error;
    private int status;

    /**
     * @param e The original Exception thrown when trying to deposit.
     */
    public DepositionException(Exception e) {
        super(e);
    }

    /**
     * @param msg The exception message.
     */
    public DepositionException(String msg) {
        super(msg);
    }

    /**
     * @param status The HTTP status code.
     * @param nestedErrorDoc A JSON structure from the 3rd party service detailing any errors.
     */
    public DepositionException(int status, JsonNode nestedErrorDoc) {
        this.status = status;
        this.error = nestedErrorDoc;
    }

    /**
     * A JSON error document that the external repository responded with, if applicable.
     */
    public JsonNode getError() {
        return error;
    }

    /**
     * The HTTP status code that the external repository responded with, if applicable.
     */
    public int getStatus() {
        return status;
    }
}
