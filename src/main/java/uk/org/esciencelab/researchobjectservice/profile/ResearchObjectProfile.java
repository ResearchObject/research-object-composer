package uk.org.esciencelab.researchobjectservice.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cascade;
import org.json.JSONObject;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import javax.persistence.*;
import java.util.List;

@Entity
public class ResearchObjectProfile {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @OneToMany(targetEntity=ResearchObject.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_name")
    private List<ResearchObject> researchObjects;
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
    public JSONObject getTemplate() {
        return getSchemaWrapper().getTemplate();
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
}
