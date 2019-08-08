package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.validation.ProfileValidationException;
import uk.org.esciencelab.researchobjectservice.validation.ResearchObjectValidator;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * A representation of a RO produced by the composer.
 * Links to a ResearchObjectProfile using "profile_name" as the foreign key, which links to the profile's "name".
 * Has JSON "content" which is stored in a Postgres "jsonb" field.
 */
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonStringType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Entity
public class ResearchObject {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_name", referencedColumnName = "name")
    private ResearchObjectProfile profile;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ObjectNode content;

    enum State {
        OPEN,
        VALIDATED,
        DEPOSITED
    }
    private State state = State.OPEN;

    private String contentSha256;

    private URI depositionUrl;

    private Date createdAt;

    private Date modifiedAt;

    private Date depositedAt;

    public ResearchObject() { }

    /**
     * Create an RO with the given profile. Initializes the content to the skeleton template.
     * @param profile
     */
    public ResearchObject(ResearchObjectProfile profile) {
        setProfile(profile);
    }

    public long getId() {
        return id;
    }

    @JsonIgnore
    public String getFriendlyId() {
        return getProfileName() + "-" + getId();
    }

    public String getProfileName() { return getProfile().getName(); }

    public ResearchObjectProfile getProfile() {
        return this.profile;
    }

    /**
     * Set the profile of the RO. This will reset the RO's content to the profile's template.
     * @param profile
     */
    public void setProfile(ResearchObjectProfile profile) {
        this.profile = profile;
        if (this.content == null) {
            this.content = getTemplate();
        }
    }

    public ObjectNode getContent() { return content; }

    /**
     * Replace RO content without validating first.
     * @param content
     */
    public void setContent(ObjectNode content) {
        this.content = content;
    }

    /**
     * Set content, but perform validation beforehand.
     * @param content
     */
    public void setAndValidateContent(ObjectNode content) throws ProfileValidationException {
        getValidator().validate(content);
        setContent(content);
    }

    @JsonProperty("checksum")
    public String getContentSha256() { return this.contentSha256; }

    /**
     * Compute a SHA-256 checksum of the RO's JSON content.
     * @return The SHA-256 digest as a hex string.
     * @throws NoSuchAlgorithmException
     * @throws JsonProcessingException
     */
    public String computeContentSha256() throws NoSuchAlgorithmException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(om.writeValueAsBytes(getContent()));
        byte[] bytes = digest.digest();

        return DatatypeConverter.printHexBinary(bytes);
    }

    /**
     * Compute and update the SHA-256 checksum of the content.
     */
    public void updateContentSha256() {
        try {
            this.contentSha256 = this.computeContentSha256();
        } catch (NoSuchAlgorithmException e) {
        } catch (JsonProcessingException e) {
        }
    }

    public boolean contentHasChanged() {
        try {
            return this.contentSha256.equals(this.computeContentSha256());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get a specific field from the JSON content.
     * @param name The name of the field to get.
     * @return
     */
    public JsonNode getField(String name) {
        return getContent().get(name);
    }

    /**
     * Set a given field to the given value.
     * @param field
     * @param value
     * @throws ProfileValidationException
     */
    public void setField(String field, JsonNode value) throws ProfileValidationException {
        getValidator().validateFieldValue(field, value);

        getContent().set(field, value);
    }

    /**
     * Append the given value to the given list field.
     * @param field
     * @param value
     * @throws ProfileValidationException
     */
    public void appendToField(String field, JsonNode value) throws ProfileValidationException {
        ArrayNode arr = (ArrayNode) getField(field);

        getValidator().validateListFieldValue(field, value);

        arr.add(value);
    }

    /**
     * Reset the given field to its initial value (from the template).
     * @param field
     */
    public void clearField(String field) {
        getContent().set(field, getTemplate().get(field));
    }

    /**
     * Apply a JSON patch to the content.
     * @param jsonPatch
     * @throws IOException
     * @throws JsonPatchException
     */
    public void patchContent(JsonNode jsonPatch) throws IOException, JsonPatchException {
        JsonNode contentNode = getContent();

        JsonPatch patch = JsonPatch.fromJson(jsonPatch);
        JsonNode patchedObject = patch.apply(contentNode);

        getValidator().validate(patchedObject);

        this.setContent((ObjectNode) patchedObject);
    }

    /**
     * Can this field be appended to? (Is it a list?)
     * @param field
     * @return
     */
    public boolean supportsAppend(String field) {
        return getProfile().canAppend(field);
    }

    /**
     * Validate the RO against its profile.
     */
    public void validate() throws ProfileValidationException {
        getValidator().validate(getContent());
    }

    /**
     * Validate the RO and mark it as validated.
     */
    public void validateAndUpdateState() throws ProfileValidationException {
        validate();
        this.updateContentSha256();
        this.state = State.VALIDATED;
    }

    @JsonIgnore
    public URI getDepositionUrl() {
        return this.depositionUrl;
    }

    public void setDepositionUrl(URI depositionUrl) {
        this.depositionUrl = depositionUrl;
        this.state = State.DEPOSITED;
        this.depositedAt = new Date();
    }

    public boolean isMutable() {
        return this.state != State.DEPOSITED;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public Date getModifiedAt() {
        return this.modifiedAt;
    }

    public Date getDepositedAt() {
        return this.depositedAt;
    }

    private ResearchObjectValidator getValidator() {
        return getProfile().getValidator();
    }

    private ObjectNode getTemplate() {
        return getProfile().getTemplate();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = new Date();
    }
}
