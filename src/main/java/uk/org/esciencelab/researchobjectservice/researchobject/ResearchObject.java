package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.data.annotation.Id;
import uk.org.esciencelab.researchobjectservice.profile.Field;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.io.OutputStream;
import java.util.HashMap;

public class ResearchObject {
    @Id
    private String id;
    private String profileId;
    private ResearchObjectProfile profile;
    private HashMap<String, Object> fields;

    public ResearchObject() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileId() {
        return profileId;
    }

    private ResearchObjectProfile getProfile() {
        return this.profile;
    }

    public void setProfile(ResearchObjectProfile profile) {
        this.profileId = profile.getId();
        this.profile = profile;
    }

    public HashMap<String, Object> getFields() {
        if (fields == null) {
            this.fields = new HashMap(10);
        }
        return fields;
    }

    public void setFields(HashMap<String, Object> hash) { this.fields = hash; }

    public Object getField(String name) {
        return getFields().get(name);
    }

    public boolean setField(String name, Object value) {
        Field field = getProfile().getField(name);
        if (field != null) {
            try {
                getFields().put(name, field.buildValue(value));
            } catch (Exception e) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public void bundle(OutputStream outputStream) throws Exception {
        new ResearchObjectBundler(this).bundle(outputStream);
    }
}
