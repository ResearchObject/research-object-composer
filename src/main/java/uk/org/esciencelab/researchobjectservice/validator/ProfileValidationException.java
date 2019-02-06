package uk.org.esciencelab.researchobjectservice.validator;

import org.json.JSONObject;

public class ProfileValidationException extends RuntimeException {
    private JSONObject json;

    public ProfileValidationException(JSONObject json) {
        this.json = json;
    }

    public JSONObject toJSON() {
        return this.json;
    };
}
