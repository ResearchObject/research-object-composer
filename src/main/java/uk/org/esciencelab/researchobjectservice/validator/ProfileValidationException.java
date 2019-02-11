package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;

import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.jsonNode;

public class ProfileValidationException extends RuntimeException {
    private JsonNode jsonNode;

    public ProfileValidationException(JSONObject json) {
        this.jsonNode = jsonNode(json);
    }

    public JsonNode toJsonNode() {
        return this.jsonNode;
    };
}
