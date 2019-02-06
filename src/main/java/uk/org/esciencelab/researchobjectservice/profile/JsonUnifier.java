package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.IOException;

public class JsonUnifier {
    public static JSONObject jsonObject(JsonNode jsonNode) {
        return new JSONObject(jsonNode.toString());
    }

    public static JSONObject jsonObject(JSONObject jsonObject) {
        return jsonObject;
    }

    public static JsonNode jsonNode(JSONObject jsonObject) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonObject.toString());
        } catch (IOException e) {
            return null;
        }
    }

    public static JsonNode jsonNode(JsonNode jsonNode) {
        return jsonNode;
    }
}
