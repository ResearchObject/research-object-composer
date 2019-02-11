package uk.org.esciencelab.researchobjectservice.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObjectNotFoundException;

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
