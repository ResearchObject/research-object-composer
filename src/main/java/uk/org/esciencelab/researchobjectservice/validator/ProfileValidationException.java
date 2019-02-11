package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;

import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.jsonNode;

public class ProfileValidationException extends RuntimeException {
    private JSONObject json;

    public ProfileValidationException(JSONObject json) {
        this.json = json;
    }

    public JSONObject toJSON() {
        return this.json;
    };

    public JsonNode toJsonNode() {
        return jsonNode(this.json);
    };
}
