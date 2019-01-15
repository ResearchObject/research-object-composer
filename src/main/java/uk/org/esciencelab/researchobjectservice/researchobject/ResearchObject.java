package uk.org.esciencelab.researchobjectservice.researchobject;

import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.StringSchema;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.util.HashMap;

@Document
public class ResearchObject {
    @Id
    private String id;
    @DBRef
    private ResearchObjectProfile profile;
    private HashMap<String, Object> fields;

    public ResearchObject() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProfileId() {
        return getProfile().getId();
    }

    public ResearchObjectProfile getProfile() {
        return this.profile;
    }

    public void setProfile(ResearchObjectProfile profile) {
        this.profile = profile;
    }

    public HashMap<String, Object> getFields() {
        if (fields == null) {
            this.fields = getProfile().getTemplate();
        }
        return fields;
    }

    public void setFields(HashMap<String, Object> hash) { this.fields = hash; }

    public Object getField(String name) {
        return getFields().get(name);
    }

    public void setField(String field, String value) {
        Schema schema = getFieldSchema(field);
        Object obj = asJSONObject(schema, value);

        schema.validate(obj);

        getFields().put(field, obj);
    }

    public void appendToField(String field, String value) {
        JSONArray arr = (JSONArray) getField(field);

        arr.put(arr.length(), value);
    }

    public void clearField(String field) {
        Object blank = getProfile().getBlankField(field);

        getFields().put(field, blank);
    }

    private Object asJSONObject(Schema schema, String value) {
        Object obj;

        // TODO: Find a better way of doing this
        if (schema instanceof ArraySchema) {
            obj = new JSONArray(value);
        } else if (schema instanceof StringSchema) {
            obj = value;
        } else {
            obj = new JSONObject(value);
        }

        return obj;
    }

    public Schema getFieldSchema(String field) {
        return getProfile().getFieldSchema(field);
    }

    public boolean supportsAppend(String field) {
        return getFieldSchema(field) instanceof ArraySchema;
    }
}
