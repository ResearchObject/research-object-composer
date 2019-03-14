package uk.org.esciencelab.researchobjectservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A utility class to help convert between Jackson and org.json JSON representations.
 */
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

    /**
     * Return an org.json compatible object from a Jackson JsonNode value.
     * @param value
     * @return
     */
    public static Object objectFromJsonNode(JsonNode value) {
        return new JSONTokener(value.toString()).nextValue();
    }
}
