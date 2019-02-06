package uk.org.esciencelab.researchobjectservice.profile;

import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class SchemaWrapper {
    private ObjectSchema objectSchema;

    public SchemaWrapper(String schemaPath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(schemaPath);
        JSONObject schemaJson = new JSONObject(new JSONTokener(is));
        this.objectSchema = (ObjectSchema) SchemaLoader.load(schemaJson);
    }

    public ObjectSchema getObjectSchema() {
        return objectSchema;
    }

    public String [] getFields() {
        Set<String> fieldNames = getObjectSchema().getPropertySchemas().keySet();
        return fieldNames.toArray(new String[0]);
    }

    public JSONObject getTemplate() {
        Map<String, Schema> schemaMap = getObjectSchema().getPropertySchemas();
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
            o.put(entry.getKey(), getBlankField(entry.getValue()));
        }

        return o;
    }

    public Object getBlankField(Schema fieldSchema) {
        Object value;
        if (fieldSchema instanceof ArraySchema) {
            value = new JSONArray();
        } else if (fieldSchema instanceof ObjectSchema) {
            value = new JSONObject();
        } else {
            value = JSONObject.NULL;
        }

        return value;
    }

    public Schema getFieldSchema(String field) {
        Schema schema = getObjectSchema().getPropertySchemas().get(field);

        while (schema instanceof ReferenceSchema) {
            schema = ((ReferenceSchema) schema).getReferredSchema();
        }

        return schema;
    }

    public Schema getListFieldItemSchema(String field) {
        ArraySchema schema = (ArraySchema) getFieldSchema(field);

        return schema.getAllItemSchema();
    }

    public boolean canAppend(String field) {
        return getFieldSchema(field) instanceof ArraySchema;
    }
}
