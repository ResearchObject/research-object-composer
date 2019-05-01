package uk.org.esciencelab.researchobjectservice.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Class for deciding how the server should respond when a ProfileValidationException occurs.
 */
@ControllerAdvice
@RequestMapping(produces = "application/json")
public class ProfileValidationAdvice {
    @ResponseBody
    @ExceptionHandler(ProfileValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    JsonNode profileValidationExceptionHandler(ProfileValidationException ex) {
        return ex.toJsonNode();
    }
}
