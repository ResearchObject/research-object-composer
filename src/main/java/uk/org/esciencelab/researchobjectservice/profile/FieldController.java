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

        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PutMapping("/research_objects/{id}/{field}")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable String id, @PathVariable String field, @RequestBody String value) {
        ResearchObject researchObject = getResearchObject(id);

        researchObject.setField(field, value);

        researchObjectRepository.save(researchObject);

        return ResponseEntity.noContent().build();
    }


    private ResearchObject getResearchObject(String id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }
}
