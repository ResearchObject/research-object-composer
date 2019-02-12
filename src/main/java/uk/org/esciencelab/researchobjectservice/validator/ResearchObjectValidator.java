package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.org.esciencelab.researchobjectservice.profile.SchemaWrapper;

import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.jsonObject;

public class ResearchObjectValidator {
    private SchemaWrapper schemaWrapper;

    public ResearchObjectValidator(SchemaWrapper schemaWrapper) {
        this.schemaWrapper = schemaWrapper;
    }

    public void validate(JsonNode content) throws ProfileValidationException {
        validate(jsonObject(content));
    }

    public void validate(JSONObject content) throws ProfileValidationException {
        try {
            getSchemaWrapper().getObjectSchema().validate(content);
        } catch (ValidationException e) {
            throw new ProfileValidationException(e, getSchemaWrapper());
        }
    }

    public void validate(String content) throws ProfileValidationException {
        validate(new JSONObject(content));
    }

    public void validateFieldValue(String field, JsonNode value) throws ProfileValidationException {
        try {
            getSchemaWrapper().getFieldSchema(field).validate(convertValueForValidate(value));
        } catch (ValidationException e) {
            throw new ProfileValidationException(e, getSchemaWrapper());
        }
    }

    public void validateListFieldValue(String field, JsonNode value) throws ProfileValidationException {
        try {
            getSchemaWrapper().getListFieldItemSchema(field).validate(convertValueForValidate(value));
        } catch (ValidationException e) {
            throw new ProfileValidationException(e, getSchemaWrapper());
        }
    }

    private SchemaWrapper getSchemaWrapper() {
        return this.schemaWrapper;
    }

    private Object convertValueForValidate(JsonNode value) {
        return new JSONTokener(value.toString()).nextValue();
    }
}
