package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.MethodNotAllowedException;

import java.io.IOException;

/**
 * A controller to handle manipulation of a ResearchObject's content.
 * Allows:
 * <ul>
 *     <li>GET/PUT of the entire JSON structure.</li>
 *     <li>GET/PUT/POST/DELETE on individual, top-level fields in the JSON.</li>
 *     <li>PATCH using JSON-Patch to alter any part of the JSON.</li>
 * </ul>
 *
 * Appropriate validation is performed for each request.
 */
@RestController
public class ContentController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;

    @GetMapping("/research_objects/{id}/content/{field}")
    public ResponseEntity<Object> getResearchObjectField(@PathVariable long id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkField(researchObject, field);

        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PutMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable long id, @PathVariable String field, @RequestBody JsonNode value) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);
        checkField(researchObject, field);

        researchObject.setField(field, value);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PostMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> appendToResearchObjectField(@PathVariable long id, @PathVariable String field, @RequestBody JsonNode value) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);
        checkField(researchObject, field);
        if (!(researchObject.supportsAppend(field))) {
            throw new MethodNotAllowedException("POST", null);
        }

        researchObject.appendToField(field, value);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getField(field));
    }

    @DeleteMapping(value="/research_objects/{id}/content/{field}", produces="application/json")
    public ResponseEntity<Object> clearResearchObjectField(@PathVariable long id, @PathVariable String field) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);
        checkField(researchObject, field);

        researchObject.clearField(field);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PatchMapping(value="/research_objects/{id}/content", produces="application/json")
    public ResponseEntity<Object> patchResearchObjectContent(@PathVariable long id, @RequestBody JsonNode jsonPatch) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);
        try {
            researchObject.patchContent(jsonPatch);
            researchObjectRepository.save(researchObject);
            return ResponseEntity.ok(researchObject.getContent());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("JSON parse error");
        } catch (JsonPatchException e) {
            return ResponseEntity.badRequest().body("Invalid JSON patch");
        }
    }

    @GetMapping(value="/research_objects/{id}/content", produces="application/json")
    public ResponseEntity<Object> getResearchObjectContent(@PathVariable long id) {
        ResearchObject researchObject = getResearchObject(id);
        return ResponseEntity.ok(researchObject.getContent());
    }

    @PutMapping(value="/research_objects/{id}/content", produces="application/json")
    public ResponseEntity<Object> getResearchObjectContent(@PathVariable long id, @RequestBody JsonNode content) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);
        researchObject.setAndValidateContent((ObjectNode) content);
        researchObjectRepository.save(researchObject);
        return ResponseEntity.ok(researchObject.getContent());
    }

    private ResearchObject getResearchObject(long id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }

    private void checkField(ResearchObject researchObject, String field) {
        if (!researchObject.getProfile().hasField(field)) {
            throw new FieldNotFoundException(field);
        }
    }

    private void checkMutable(ResearchObject researchObject) {
        if (!researchObject.isMutable()) {
            throw new ImmutableResearchObjectException();
        }
    }
}
