package uk.org.esciencelab.researchobjectservice;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResearchObjectFieldsConverter implements Converter<JSONObject$Null, DBObject> {
    @Override
    public DBObject convert(JSONObject fields) {
        return new BasicDBObject(fields.toMap());
    }
}
