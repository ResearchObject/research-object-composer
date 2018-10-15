package uk.org.esciencelab.researchobjectservice.profile;

import java.lang.reflect.*;

public class Field {
    private String name;
    private String type;

    public Field(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object buildValue(Object val) throws Exception {
        Class c = Class.forName(type);
        Constructor ct = c.getConstructor(val.getClass());
        return ct.newInstance(val);
    }
}
