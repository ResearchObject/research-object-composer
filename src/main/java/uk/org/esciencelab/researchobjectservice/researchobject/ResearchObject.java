package uk.org.esciencelab.researchobjectservice.researchobject;

import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ReferenceSchema;
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
        Object obj;

        Schema s = getFieldSchema(field);
        if (s instanceof ReferenceSchema) {
            s = ((ReferenceSchema) s).getReferredSchema();
        }

        // TODO: Find a better way of doing this
        if (s instanceof ArraySchema) {
            obj = new JSONArray(value);
        } else if (s instanceof StringSchema) {
            obj = value;
        } else {
            obj = new JSONObject(value);
        }

        getFieldSchema(field).validate(obj);

        getFields().put(field, obj);
    }

    private Schema getFieldSchema(String field) {
        return getProfile().getSchema().getPropertySchemas().get(field);
    }
}
