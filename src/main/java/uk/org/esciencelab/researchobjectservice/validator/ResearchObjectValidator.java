package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.SchemaWrapper;

import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.jsonObject;
import static uk.org.esciencelab.researchobjectservice.util.JsonUnifier.objectFromJsonNode;

/**
 * A class for validating a research objects using a provided ResearchObjectProfile,
 * raising a ProfileValidationException if the given content is not valid.
 *
 * @see ProfileValidationException
 * @see ResearchObjectProfile
 */
public class ResearchObjectValidator {
    private ResearchObjectProfile profile;

    /**
     * @param profile The profile to validate against.
     */
    public ResearchObjectValidator(ResearchObjectProfile profile) {
        this.profile = profile;
    }

    /**
     * Validate the given ResearchObject content. Converts to org.json JSONObject beforehand.
     * @param content The ResearchObject's content to validate, as Jackson JSON.
     * @throws ProfileValidationException
     */
    public void validate(JsonNode content) throws ProfileValidationException {
        validate(jsonObject(content));
    }

    /**
     * Validate the given ResearchObject content.
     * @param content The ResearchObject's content to validate, as org.json JSON.
     * @throws ProfileValidationException
     */
    public void validate(JSONObject content) throws ProfileValidationException {
        try {
            getSchemaWrapper().getObjectSchema().validate(content);
        } catch (ValidationException e) {
            throw new ProfileValidationException(e, this.profile);
        }
    }

    /**
     * Validate a single field of a ResearchObject.
     * @param field The field name.
     * @param value Its value.
     * @throws ProfileValidationException
     */
    public void validateFieldValue(String field, JsonNode value) throws ProfileValidationException {
        try {
            getSchemaWrapper().getFieldSchema(field).validate(objectFromJsonNode(value));
        } catch (ValidationException e) {
            throw new ProfileValidationException(e, this.profile);
        }
    }

    /**
     * Validate a single list field of a ResearchObject.
     * @param field The field name.
     * @param value Its value.
     * @throws ProfileValidationException
     */
    public void validateListFieldValue(String field, JsonNode value) throws ProfileValidationException {
        try {
            getSchemaWrapper().getListFieldItemSchema(field).validate(objectFromJsonNode(value));
        } catch (ValidationException e) {
            throw new ProfileValidationException(e, this.profile);
        }
    }

    private SchemaWrapper getSchemaWrapper() {
        return this.profile.getSchemaWrapper();
    }
}
