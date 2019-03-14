package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper class to try and avoid tight coupling to the org.everit.json.schema library.
 */
public class SchemaWrapper {
    private ObjectSchema objectSchema;
    private String schemaPath;

    /**
     * @param schemaPath A path to the schema, e.g. /schemas/draft_task.schema.json
     */
    public SchemaWrapper(String schemaPath) {
        this.schemaPath = schemaPath;
        InputStream is = getClass().getClassLoader().getResourceAsStream("public" + schemaPath);
        JSONObject schemaJson = new JSONObject(new JSONTokener(is));
        this.objectSchema = (ObjectSchema) SchemaLoader.builder()
                .schemaClient(SchemaClient.classPathAwareClient())
                .schemaJson(schemaJson)
                .resolutionScope("classpath://public/")
                .build().load().build();
    }


    public String getSchemaPath() {
        return this.schemaPath;
    }

    public ObjectSchema getObjectSchema() {
        return this.objectSchema;
    }

    public String [] getFields() {
        Set<String> fieldNames = getObjectSchema().getPropertySchemas().keySet();
        return fieldNames.toArray(new String[0]);
    }

    /**
     * Get a skeleton JSON object, to be applied to new research objects that use this schema.
     * @return The template JSON object.
     */
    public JSONObject getTemplate() {
        Map<String, Schema> schemaMap = getObjectSchema().getPropertySchemas();
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
            o.put(entry.getKey(), getBlankValue(entry.getValue().getClass()));
        }

        return o;
    }

    /**
     * Return the default "blank" value for the schema type.
     * For an array: [], for an object: {}, for anything else: null
     * @param schemaClass The schema class to check.
     * @return
     */
    public Object getBlankValue(Class schemaClass) {
        Object value;
        if (schemaClass.equals(ArraySchema.class)) {
            value = new JSONArray();
        } else if (schemaClass.equals(ObjectSchema.class)) {
            value = new JSONObject();
        } else {
            value = JSONObject.NULL;
        }

        return value;
    }

    /**
     * Get the schema that applies to the given field.
     * @param field The name of the field.
     * @return
     */
    public Schema getFieldSchema(String field) {
        Schema schema = getObjectSchema().getPropertySchemas().get(field);

        while (schema instanceof ReferenceSchema) {
            schema = ((ReferenceSchema) schema).getReferredSchema();
        }

        return schema;
    }

    /**
     * Get the schema that applies to items within the given list field.
     * @param field The name of the list field.
     * @return
     */
    public Schema getListFieldItemSchema(String field) {
        ArraySchema schema = (ArraySchema) getFieldSchema(field);

        return schema.getAllItemSchema();
    }

    /**
     * Can this field have values appended to it? (is it a list?)
     * @param field The name of the field.
     * @return
     */
    public boolean canAppend(String field) {
        return getFieldSchema(field) instanceof ArraySchema;
    }

    public JsonNode toJsonNode() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(getObjectSchema().toString(), JsonNode.class);
    }
}
