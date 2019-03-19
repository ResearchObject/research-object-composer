package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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
import uk.org.esciencelab.researchobjectservice.validator.ProfileValidationException;
import uk.org.esciencelab.researchobjectservice.validator.ResearchObjectValidator;

import javax.persistence.*;
import java.io.IOException;

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
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_name", referencedColumnName = "name")
    private ResearchObjectProfile profile;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ObjectNode content;

    public ResearchObject() { }

    /**
     * Create an RO with the given profile. Initializes the content to the skeleton template.
     * @param profile
     */
    public ResearchObject(ResearchObjectProfile profile) {
        setProfile(profile);
    }

    public Long getId() {
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
    public void validate() {
        getValidator().validate(getContent());
    }

    private ResearchObjectValidator getValidator() {
        return getProfile().getValidator();
    }

    private ObjectNode getTemplate() {
        return getProfile().getTemplate();
    }
}
