package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.MethodNotAllowedException;

@RestController
public class ContentController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;

    @GetMapping("/research_objects/{id}/content/{field}")
    public ResponseEntity<Object> getResearchObjectField(@PathVariable Long id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PutMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable Long id, @PathVariable String field, @RequestBody JsonNode value) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        researchObject.setField(field, value);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PostMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> appendToResearchObjectField(@PathVariable Long id, @PathVariable String field, @RequestBody JsonNode value) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);
        if (!(researchObject.supportsAppend(field))) {
            throw new MethodNotAllowedException("POST", null);
        }

        researchObject.appendToField(field, value);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getField(field));
    }

    @DeleteMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> clearResearchObjectField(@PathVariable Long id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        researchObject.clearField(field);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PatchMapping(value="/research_objects/{id}/content", produces="application/json")
    public ResponseEntity<Object> patchResearchObjectContent(@PathVariable Long id, @RequestBody JsonNode jsonPatch) throws Exception {
        ResearchObject researchObject = getResearchObject(id);
        researchObject.patchContent(jsonPatch);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getContent());
    }

    @GetMapping(value="/research_objects/{id}/content", produces="application/json")
    public ResponseEntity<Object> getResearchObjectContent(@PathVariable Long id) {
        ResearchObject researchObject = getResearchObject(id);
        return ResponseEntity.ok(researchObject.getContent());
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
