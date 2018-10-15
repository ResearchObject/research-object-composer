package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.data.annotation.Id;

public class ResearchObjectProfile {
    @Id
    private String id;
    private Field[] fields;

    public ResearchObjectProfile() {}

    public ResearchObjectProfile(String id, Field[] fields) {
        super();
        this.id = id;
        this.fields = fields;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Field[] getFields() {
        return this.fields;
    }

    public boolean hasField(String name) {
        return getField(name) != null;
    }

    public Field getField(String name) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }
}
