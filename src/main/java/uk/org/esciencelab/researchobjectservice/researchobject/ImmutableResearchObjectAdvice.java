package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Class for deciding how the server should respond when a ImmutableResearchObjectException occurs.
 */
@ControllerAdvice
public class ImmutableResearchObjectAdvice {
    @ResponseBody
    @ExceptionHandler(ImmutableResearchObjectException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    String researchObjectImmutableHandler(ImmutableResearchObjectException ex) {
        return ex.getMessage();
    }
}
