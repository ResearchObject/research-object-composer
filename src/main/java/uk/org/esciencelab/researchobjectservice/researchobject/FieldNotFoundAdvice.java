package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Class for deciding how the server should respond when a FieldNotFoundException occurs.
 */
@ControllerAdvice
public class FieldNotFoundAdvice {
    @ResponseBody
    @ExceptionHandler(FieldNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String fieldNotFoundHandler(FieldNotFoundException ex) { return ex.getMessage(); }
}
