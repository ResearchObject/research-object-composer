package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.annotations.NaturalId;
import uk.org.esciencelab.researchobjectservice.validation.ResearchObjectValidator;

import javax.persistence.*;
import java.io.Serializable;

import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.jsonNode;


/**
 * A representation of a RO profile.
 * Consists of a name (used as the key for lookups) and a JSON schema.
 */
@Entity
public class ResearchObjectProfile implements Serializable { // Need to implement Serializable here because of https://hibernate.atlassian.net/browse/HHH-7668
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
//    @OneToMany(targetEntity=ResearchObject.class, fetch = FetchType.LAZY)
//    @JoinColumn(name = "profile_name", referencedColumnName = "name")
//    private List<ResearchObject> researchObjects;
    @NaturalId
    @Column(unique=true)
    private String name;
    private String schemaPath;

    public ResearchObjectProfile() {}

    /**
     * @param name The unique name of the profile.
     * @param schemaPath A path to the JSON schema.
     */
    public ResearchObjectProfile(String name, String schemaPath) {
        super();
        this.name = name;
        this.schemaPath = schemaPath;
    }

    public long getId() {
        return this.id;
    }

    public String getName() { return this.name; }

    @JsonIgnore
    public SchemaWrapper getSchemaWrapper() {
        return new SchemaWrapper(this.schemaPath);
    }

    /**
     * Get a skeleton JSON object, to be applied to new research objects that use this profile.
     * @return The template JSON object.
     */
    @JsonIgnore
    public ObjectNode getTemplate() {
        return (ObjectNode) jsonNode(getSchemaWrapper().getTemplate());
    }

    /**
     * @return An array of top-level field names for this profile.
     */
    public String [] getFields() {
        return getSchemaWrapper().getFields();
    }

    /**
     * Is the given field a valid field for this profile?
     * @param field The field name.
     * @return
     */
    public boolean hasField(String field) {
        for (String f : getFields()) {
            if (field.equals(f)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Can this field have values appended to it? (is it a list?)
     * @param field The name of the field.
     * @return
     */
    public boolean canAppend(String field) {
        return getSchemaWrapper().canAppend(field);
    }

    @JsonIgnore
    public ResearchObjectValidator getValidator() {
        return new ResearchObjectValidator(this);
    }

    @JsonIgnore
    public String getSchemaPath() {
        return this.schemaPath;
    }
}
