package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Document
public class ResearchObjectProfile {
    @Id
    private String id;
    private String schemaPath;
    private String [] fields;

    public ResearchObjectProfile() {}

    public ResearchObjectProfile(String id, String schemaPath) {
        super();
        this.id = id;
        this.schemaPath = schemaPath;
        InputStream is = getClass().getClassLoader().getResourceAsStream(schemaPath);
        JSONObject rawSchema = new JSONObject(new JSONTokener(is));
        JSONObject properties = rawSchema.getJSONObject("properties");
        this.fields = properties.keySet().toArray(new String[properties.keySet().size()]);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public ObjectSchema getSchema() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(schemaPath);
        JSONObject rawSchema = new JSONObject(new JSONTokener(is));
        return (ObjectSchema) SchemaLoader.load(rawSchema);
    }

    public String [] getFields() {
        return this.fields;
    }

    public boolean hasField(String field) {
        for (String f : this.fields) {
            if (field.equals(f)) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public HashMap<String, Object> getTemplate() {
        Map<String, Schema> schemaMap = getSchema().getPropertySchemas();
        HashMap<String, Object> h = new HashMap(fields.length);
        for (String f : fields) {
            Schema fieldSchema = schemaMap.get(f);
            Object value;
            if (fieldSchema instanceof ArraySchema) {
                value = new JSONArray();
            } else {
                value = null;
            }

            h.put(f, value);
        }

        return h;
    }
}
