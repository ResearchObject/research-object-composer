package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.org.esciencelab.researchobjectservice.profile.SchemaWrapper;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import static uk.org.esciencelab.researchobjectservice.profile.JsonUnifier.jsonObject;

public class ResearchObjectValidator {

    private ResearchObject researchObject;
    private SchemaWrapper schemaWrapper;

    public ResearchObjectValidator(ResearchObject researchObject) {
        this.researchObject = researchObject;
        this.schemaWrapper = researchObject.getProfile().getSchemaWrapper();
    }

    public SchemaWrapper getSchemaWrapper() {
        return this.schemaWrapper;
    }

    public void validate(JsonNode content) throws ProfileValidationException {
        validate(jsonObject(content));
    }

    public void validate(JSONObject content) throws ProfileValidationException {
        try {
            getSchemaWrapper().getObjectSchema().validate(content);
        } catch (ValidationException e) {
            throw new ProfileValidationException(e.toJSON());
        }
    }

    public void validateFieldValue(String field, String value) throws ProfileValidationException {
        try {
            getSchemaWrapper().getFieldSchema(field).validate(convertValueForValidate(value));
        } catch (ValidationException e) {
            throw new ProfileValidationException(e.toJSON());
        }
    }

    public void validateListFieldValue(String field, String value) throws ProfileValidationException {
        try {
            getSchemaWrapper().getListFieldItemSchema(field).validate(convertValueForValidate(value));
        } catch (ValidationException e) {
            throw new ProfileValidationException(e.toJSON());
        }
    }

    private Object convertValueForValidate(String value) {
        return new JSONTokener(value).nextValue();
    }
}
