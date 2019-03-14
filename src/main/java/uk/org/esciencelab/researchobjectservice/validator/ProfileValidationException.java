package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.everit.json.schema.ValidationException;
import uk.org.esciencelab.researchobjectservice.profile.SchemaWrapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An exception that wraps ValidationException, adding a method to generate a Jackson JsonNode object detailing
 * the error.
 */
public class ProfileValidationException extends RuntimeException {

    private ValidationException ve;
    private SchemaWrapper schemaWrapper;

    /**
     * @param e The original ValidationException thrown by the JSON schema validator.
     * @param schemaWrapper The schema wrapper object for the profile that was violated.
     */
    public ProfileValidationException(ValidationException e, SchemaWrapper schemaWrapper) {
        this.ve = e;
        this.schemaWrapper = schemaWrapper;
    }

    /**
     * Create a (Jackson) JSON object containing details of the validation error.
     * Does some re-writing of the schema location to ensure it is a URL that the client can resolve.
     *
     * @return The error document.
     */
    public JsonNode toJsonNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        if (ve.getKeyword() != null) {
            node.put("keyword", ve.getKeyword());
        }
        if (ve.getPointerToViolation() == null) {
            node.putNull("pointerToViolation");
        } else {
            node.put("pointerToViolation", ve.getPointerToViolation());
        }
        if (ve.getMessage() != null) {
            node.put("message", ve.getMessage());
        }
        List<JsonNode> causeJsons = ve.getCausingExceptions().stream()
                .map(e -> new ProfileValidationException(e, this.schemaWrapper).toJsonNode())
                .collect(Collectors.toList());
        ArrayNode causingExceptions = node.putArray("causingExceptions");
        causingExceptions.addAll(causeJsons);

        if (ve.getSchemaLocation() != null) {
            String actualSchemaLocation = ve.getSchemaLocation();
            // Remove the classpath part of the schemaLocation since the client cannot resolve it.
            actualSchemaLocation = actualSchemaLocation.replaceFirst("classpath://public", "");
            if (actualSchemaLocation.charAt(0) == '#') {
                // Add the schema path if there was none specified.
                actualSchemaLocation = actualSchemaLocation.replaceFirst("#", this.schemaWrapper.getSchemaPath() + "#");
            }

            node.put("schemaLocation", actualSchemaLocation);
        }
        return node;
    }
}
