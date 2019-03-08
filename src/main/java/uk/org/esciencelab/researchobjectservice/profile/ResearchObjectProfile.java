package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.annotations.NaturalId;
import uk.org.esciencelab.researchobjectservice.validator.ResearchObjectValidator;

import javax.persistence.*;
import java.io.Serializable;

import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.jsonNode;

@Entity
// Need to implement Serializable here because of https://hibernate.atlassian.net/browse/HHH-7668
public class ResearchObjectProfile implements Serializable {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
//    @OneToMany(targetEntity=ResearchObject.class, fetch = FetchType.LAZY)
//    @JoinColumn(name = "profile_name", referencedColumnName = "name")
//    private List<ResearchObject> researchObjects;
    @NaturalId
    @Column(unique=true)
    private String name;
    private String schemaPath;

    public ResearchObjectProfile() {}

    public ResearchObjectProfile(String name, String schemaPath) {
        super();
        this.name = name;
        this.schemaPath = schemaPath;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() { return this.name; }

    @JsonIgnore
    public SchemaWrapper getSchemaWrapper() {
        return new SchemaWrapper(this.schemaPath);
    }

    @JsonIgnore
    public ObjectNode getTemplate() {
        return (ObjectNode) jsonNode(getSchemaWrapper().getTemplate());
    }

    public String [] getFields() {
        return getSchemaWrapper().getFields();
    }

    public boolean hasField(String field) {
        for (String f : getFields()) {
            if (field.equals(f)) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public ResearchObjectValidator getValidator() {
        return new ResearchObjectValidator(getSchemaWrapper());
    }

    @JsonIgnore
    public String getSchemaPath() {
        return this.schemaPath;
    }
}
