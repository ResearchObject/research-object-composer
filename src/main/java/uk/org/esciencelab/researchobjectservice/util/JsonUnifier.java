package uk.org.esciencelab.researchobjectservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.JSONObject;

public class JsonUnifier {
    public static JSONObject jsonObject(JsonNode jsonNode) {
        return new JSONObject(jsonNode.toString());
    }

    public static JSONObject jsonObject(JSONObject jsonObject) {
        return jsonObject;
    }

    public static JsonNode jsonNode(JSONObject jsonObject) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.convertValue(jsonObject, JsonNode.class);
    }

    public static JsonNode jsonNode(JsonNode jsonNode) {
        return jsonNode;
    }
}
