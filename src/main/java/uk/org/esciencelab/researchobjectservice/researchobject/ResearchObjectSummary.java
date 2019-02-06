package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

public class ResearchObjectSummary {
    private Long id;
    @JsonIgnore
    private ResearchObjectProfile profile;

    public ResearchObjectSummary(ResearchObject researchObject) {
        this.id = researchObject.getId();
        this.profile = researchObject.getProfile();
    }

    public Long getId() { return id; }

    public ResearchObjectProfile getProfile() {
        return profile;
    }

    public String getProfileName() {
        return getProfile().getName();
    }
}
