package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

public class ResearchObjectSummary {
    @Id
    private String id;
    @JsonIgnore
    @DBRef
    private ResearchObjectProfile profile;

    public ResearchObjectSummary(ResearchObject researchObject) {
        this.id = researchObject.getId();
        this.profile = researchObject.getProfile();
    }

    public String getId() {
        return id;
    }

    public ResearchObjectProfile getProfile() {
        return profile;
    }

    public String getProfileId() {
        return getProfile().getId();
    }
}
