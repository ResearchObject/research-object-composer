package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ResearchObjectNotFoundAdvice {
    @ResponseBody
    @ExceptionHandler(ResearchObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String researchObjectNotFoundHandler(ResearchObjectNotFoundException ex) {
        return ex.getMessage();
    }
}
