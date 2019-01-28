package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.StringSchema;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@Document
public class ResearchObject {
    @Id
    private String id;
    @JsonIgnore
    @DBRef
    private ResearchObjectProfile profile;
    private JSONObject fields;

    public ResearchObject() { }

    public ResearchObject(ResearchObjectProfile profile) {
        this.profile = profile;
        this.fields = getProfile().getTemplate();
    }

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
        if (this.fields == null) {
            this.fields = getProfile().getTemplate();
        }
    }

    @JsonGetter("fields")
    public Map<String, Object> getFieldsForJson() {
        Map<String, Object> m = getFields().toMap();

        convertMapToNulls(m);

        return m;
    }

    public JSONObject getFields() { return fields; }

    public void setFields(JSONObject obj) {
        this.fields = obj;
    }

    public Object getField(String name) {
        return getFields().get(name);
    }

    public void setField(String field, String value) {
        Schema schema = getFieldSchema(field);
        Object obj = new JSONTokener(value).nextValue();

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

    private void convertMapToNulls(Map<String, Object> map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if (pair.getValue() == null || pair.getValue().equals(null)) {
                pair.setValue(null);
            } else if (pair.getValue() instanceof Map) {
                convertMapToNulls((Map<String, Object>) pair.getValue());
            } else if (pair.getValue() instanceof List) {
                convertListToNulls((List<Object>) pair.getValue());
            }
//            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    private void convertListToNulls(List<Object> list) {
        ListIterator it = list.listIterator();
        while (it.hasNext()) {
            Object value = it.next();
            if (value == JSONObject.NULL) {
                it.set(null);
            } else if (value instanceof Map) {
                convertMapToNulls((Map<String, Object>) value);
            } else if (value instanceof List) {
                convertListToNulls((List<Object>) value);
            }
//            it.remove(); // avoids a ConcurrentModificationException
        }
    }

}
