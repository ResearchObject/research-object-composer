package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

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

    public JsonNode getError() {
        return error;
    }

    public int getStatus() {
        return status;
    }
}
