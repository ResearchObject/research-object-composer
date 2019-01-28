package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document
public class ResearchObjectProfile {
    @Id
    private String id;
    private JSONObject schema;

    public ResearchObjectProfile() {}

    public ResearchObjectProfile(String id, JSONObject schema) {
        super();
        this.id = id;
        this.schema = schema;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public ObjectSchema getSchema() {
        return (ObjectSchema) SchemaLoader.load(schema);
    }

    public String [] getFields() {
        JSONObject properties = schema.getJSONObject("properties");
        return properties.keySet().toArray(new String[properties.keySet().size()]);
    }

    public boolean hasField(String field) {
        for (String f : getFields()) {
            if (field.equals(f)) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public JSONObject getTemplate() {
        Map<String, Schema> schemaMap = getSchema().getPropertySchemas();
        JSONObject o = new JSONObject();
        for (String f : getFields()) {
            Schema fieldSchema = schemaMap.get(f);

            o.put(f, getBlankField(fieldSchema));
        }

        return o;
    }

    @JsonIgnore
    public Object getBlankField(String field) {
        Schema fieldSchema = getSchema().getPropertySchemas().get(field);

        return getBlankField(fieldSchema);
    }

    @JsonIgnore
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
        Schema schema = getSchema().getPropertySchemas().get(field);

        while (schema instanceof ReferenceSchema) {
            schema = ((ReferenceSchema) schema).getReferredSchema();
        }

        return schema;
    }
}
