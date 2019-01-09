package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.org.esciencelab.researchobjectservice.researchobject.*;

@RestController
public class FieldController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;

    @GetMapping("/research_objects/{id}/{field}")
    public ResponseEntity<Object> getResearchObjectField(@PathVariable String id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PutMapping("/research_objects/{id}/{field}")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable String id, @PathVariable String field, @RequestBody String value) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        if (researchObject.setField(field, value)) {
            researchObjectRepository.save(researchObject);

            return ResponseEntity.ok(researchObject.getField(field));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    private ResearchObject getResearchObject(String id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }

    private void checkField(ResearchObject researchObject, String field) {
        if (!researchObject.getProfile().hasField(field)) {
            throw new FieldNotFoundException(field);
        }
    }
}
