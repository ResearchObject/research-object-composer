package uk.org.esciencelab.researchobjectservice.profile;

import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObjectNotFoundException;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObjectRepository;


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

    @PutMapping(value="/research_objects/{id}/{field}", produces="application/json")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable String id, @PathVariable String field, @RequestBody String value) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        try {
            researchObject.setField(field, value);
            researchObjectRepository.save(researchObject);
            JSONObject jo = new JSONObject();
            jo.put(field, researchObject.getField(field));
            return ResponseEntity.ok(jo.toString());
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(e.toJSON().toString());
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
