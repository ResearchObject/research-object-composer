package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;

import java.net.URI;
import java.util.Date;

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

    public URI getDepositionUrl() {
        return researchObject.getDepositionUrl();
    }

    public Date getCreatedAt() {
        return researchObject.getCreatedAt();
    }

    public Date getModifiedAt() {
        return researchObject.getModifiedAt();
    }

    public Date getDepositedAt() {
        return researchObject.getDepositedAt();
    }
}
