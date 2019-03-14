package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

/**
 * A sparse "summary" version of a research object, to be used to serialize ROs on index views.
 */
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
