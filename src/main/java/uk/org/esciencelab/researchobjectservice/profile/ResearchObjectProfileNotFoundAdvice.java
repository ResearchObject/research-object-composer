package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Class for deciding how the server should respond when a ResearchObjectProfileNotFoundException occurs.
 */
@ControllerAdvice
public class ResearchObjectProfileNotFoundAdvice {
    @ResponseBody
    @ExceptionHandler(ResearchObjectProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String researchObjectProfileNotFoundHandler(ResearchObjectProfileNotFoundException ex) {
        return ex.getMessage();
    }
}
