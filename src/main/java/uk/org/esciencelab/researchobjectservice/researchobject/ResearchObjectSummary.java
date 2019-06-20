package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.net.URI;

/**
 * A sparse "summary" version of a Research Object, to be used to serialize ROs on index views.
 */
public class ResearchObjectSummary {
    private long id;
    @JsonIgnore
    private ResearchObject researchObject;
    @JsonIgnore
    private ResearchObjectProfile profile;

    public ResearchObjectSummary(ResearchObject researchObject) {
        this.id = researchObject.getId();
        this.profile = researchObject.getProfile();
        this.researchObject = researchObject;
    }

    public long getId() { return id; }

    public ResearchObjectProfile getProfile() {
        return profile;
    }

    public String getProfileName() {
        return getProfile().getName();
    }

    public String getContentSha256() {
        return researchObject.getContentSha256();
    }

    public URI getDepositionUrl() {
        return researchObject.getDepositionUrl();
    }
}
