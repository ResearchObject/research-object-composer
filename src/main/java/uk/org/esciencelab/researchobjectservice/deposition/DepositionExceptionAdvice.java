package uk.org.esciencelab.researchobjectservice.deposition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Class for deciding how the server should respond when a DepositionException occurs.
 */
@ControllerAdvice
@RequestMapping(produces = "application/json")
public class DepositionExceptionAdvice {
    @ResponseBody
    @ExceptionHandler(DepositionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    JsonNode depositionExceptionHandler(DepositionException ex) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.convertValue(ex, JsonNode.class);
    }
}
