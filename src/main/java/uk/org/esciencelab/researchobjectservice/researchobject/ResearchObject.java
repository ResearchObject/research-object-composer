package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.validator.ResearchObjectValidator;

import javax.persistence.*;
import java.io.IOException;
import java.util.Map;

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
    @JoinColumn(name = "profile_name")
    private ResearchObjectProfile profile;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ObjectNode content;

    public ResearchObject() { }

    public ResearchObject(ResearchObjectProfile profile) {
        setProfile(profile);
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public Long getProfileId() {
        return getProfile().getId();
    }

    public String getProfileName() { return getProfile().getName(); }

    public ResearchObjectProfile getProfile() {
        return this.profile;
    }

    public void setProfile(ResearchObjectProfile profile) {
        this.profile = profile;
        if (this.content == null) {
            this.content = getTemplate();
        }
    }

    @JsonGetter("content")
    public Map<String, Object> getContentForJson() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.convertValue(getContent(), Map.class);

        return result;
    }

    public ObjectNode getContent() { return content; }

    public void setContent(ObjectNode obj) {
        this.content = obj;
    }

    public void setContent(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        setContent(mapper.readValue(content, ObjectNode.class));
    }

    public JsonNode getField(String name) {
        return getContent().get(name);
    }

    public void setField(String field, String value) {
        getValidator().validateFieldValue(field, value);

        getContent().set(field, convertValueForSet(value));
    }

    public void appendToField(String field, String value) {
        ArrayNode arr = (ArrayNode) getField(field);

        getValidator().validateListFieldValue(field, value);

        arr.add(convertValueForSet(value));
    }

    public void clearField(String field) {
        getContent().set(field, getTemplate().get(field));
    }

    public void patchContent(String jsonPatch) throws IOException, JsonPatchException {
        JsonNode contentNode = getContent();

        ObjectMapper patchMapper = new ObjectMapper();
        JsonNode patchNode = patchMapper.readTree(jsonPatch);

        JsonPatch patch = JsonPatch.fromJson(patchNode);
        JsonNode patchedObject = patch.apply(contentNode);

        getValidator().validate(patchedObject);

        this.setContent((ObjectNode) patchedObject);
    }

    public boolean supportsAppend(String field) {
        return getProfile().getSchemaWrapper().canAppend(field);
    }

    private JsonNode convertValueForSet(String value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(value);
        } catch (IOException e) {
            return NullNode.getInstance();
        }
    }

    private ResearchObjectValidator getValidator() {
        return getProfile().getValidator();
    }

    public void validate() {
        getValidator().validate(getContent());
    }

    private ObjectNode getTemplate() {
        return getProfile().getTemplate();
    }
}
