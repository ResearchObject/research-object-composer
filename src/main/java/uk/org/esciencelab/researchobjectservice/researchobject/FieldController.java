package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.MethodNotAllowedException;
import uk.org.esciencelab.researchobjectservice.validator.ProfileValidationException;

@RestController
public class FieldController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;

    @GetMapping("/research_objects/{id}/content/{field}")
    public ResponseEntity<Object> getResearchObjectField(@PathVariable Long id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PutMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable Long id, @PathVariable String field, @RequestBody String value) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        researchObject.setField(field, value);
        researchObjectRepository.save(researchObject);
        JSONObject jo = new JSONObject();
        jo.put(field, researchObject.getField(field));
        return ResponseEntity.ok(jo.toString());
    }

    @PostMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> appendToResearchObjectField(@PathVariable Long id, @PathVariable String field, @RequestBody String value) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);
        if (!(researchObject.supportsAppend(field))) {
            throw new MethodNotAllowedException("POST", null);
        }

        researchObject.appendToField(field, value);
        researchObjectRepository.save(researchObject);
        JSONObject jo = new JSONObject();
        jo.put(field, researchObject.getField(field));
        return ResponseEntity.ok(jo.toString());
    }

    @DeleteMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> clearResearchObjectField(@PathVariable Long id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        researchObject.clearField(field);
        researchObjectRepository.save(researchObject);
        JSONObject jo = new JSONObject();
        jo.put(field, researchObject.getField(field));
        return ResponseEntity.ok(jo.toString());
    }

    @PatchMapping(value="/research_objects/{id}/content", produces="application/json")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable Long id, @RequestBody String jsonPatch) throws Exception {
        ResearchObject researchObject = getResearchObject(id);
        researchObject.patchContent(jsonPatch);
        researchObjectRepository.save(researchObject);
        JsonNode jo = researchObject.getContent();
        return ResponseEntity.ok(jo.toString());
    }

    private ResearchObject getResearchObject(Long id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }

    private void checkField(ResearchObject researchObject, String field) {
        if (!researchObject.getProfile().hasField(field)) {
            throw new FieldNotFoundException(field);
        }
    }
}
