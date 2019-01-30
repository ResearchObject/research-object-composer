package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.everit.json.schema.*;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.InputStream;
import java.util.Map;

@Document
public class ResearchObjectProfile {
    @Id
    private String id;
    private String schemaPath;

    public ResearchObjectProfile() {}

    public ResearchObjectProfile(String id, String schemaPath) {
        super();
        this.id = id;
        this.schemaPath = schemaPath;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public ObjectSchema getObjectSchema() {
        return (ObjectSchema) SchemaLoader.load(getSchema());
    }

    @JsonIgnore
    public String getSchemaPath() {
        return this.schemaPath;
    }

    @JsonIgnore
    public JSONObject getSchema() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(getSchemaPath());
        return new JSONObject(new JSONTokener(is));
    }

    public String [] getFields() {
        JSONObject properties = getSchema().getJSONObject("properties");
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
        Map<String, Schema> schemaMap = getObjectSchema().getPropertySchemas();
        JSONObject o = new JSONObject();
        for (String f : getFields()) {
            Schema fieldSchema = schemaMap.get(f);

            o.put(f, getBlankField(fieldSchema));
        }

        return o;
    }

    @JsonIgnore
    public Object getBlankField(String field) {
        Schema fieldSchema = getObjectSchema().getPropertySchemas().get(field);

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

    public void validate(JSONObject object) throws ValidationException {
        getObjectSchema().validate(object);
    }
}
