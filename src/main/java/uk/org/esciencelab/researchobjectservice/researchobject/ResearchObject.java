package uk.org.esciencelab.researchobjectservice.researchobject;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import uk.org.esciencelab.researchobjectservice.profile.Field;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ResearchObject {
    @Id
    private String id;
    private String profileId;
    private HashMap<String, Object> fields;

    @Autowired
    private ResearchObjectProfileRepository researchObjectProfileRepository;

    public ResearchObject() { }

    public ResearchObject(String id, String profileId) {
        super();
        this.fields = new HashMap(10);
        this.id = id;
        this.profileId = profileId;
    }

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
        return researchObjectProfileRepository.findById(profileId).get();
    }

    public void setProfile(ResearchObjectProfile profile) {
        this.profileId = profile.getId();
    }

    public HashMap<String, Object> getFields() {
        if (fields == null) {
            this.fields = new HashMap(10);
        }
        return fields;
    }

    public void setFields(HashMap<String, Object> hash) {
        this.fields = hash;
    }

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
        Bundle bundle = Bundles.createBundle();
        for (Map.Entry<String, Object> entry : getFields().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            URI ref = URI.create(value);
            Path out = bundle.getRoot().resolve(key);
            Bundles.setReference(out, ref);
        }
        Path path = Files.createTempFile("bundle", ".zip");
        Bundles.closeAndSaveBundle(bundle, path);
        Files.copy(path, outputStream);
    }
}
