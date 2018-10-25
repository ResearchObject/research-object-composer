package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.org.esciencelab.researchobjectservice.validator.ResearchObjectValidator;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@RestController
public class ResearchObjectController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;
    @Autowired
    private ResearchObjectResourceAssembler assembler;

    @GetMapping("/profiles/{profileId}/research_objects")
    public Resources<Resource<ResearchObject>> allForProfile(@PathVariable String profileId) {
        List<ResearchObject> allByProfileId = researchObjectRepository.findAllByProfileId(profileId);
        List<Resource<ResearchObject>> researchObjectResources = allByProfileId.stream()
                .map(assembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(researchObjectResources,
                linkTo(methodOn(ResearchObjectController.class).allForProfile(profileId)).withSelfRel());
    }

    @GetMapping("/research_objects")
    public Resources<Resource<ResearchObject>> all() {
        List<ResearchObject> all = researchObjectRepository.findAll();
        List<Resource<ResearchObject>> researchObjectResources = all.stream()
                .map(assembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(researchObjectResources,
                linkTo(methodOn(ResearchObjectController.class).all()).withSelfRel());
    }

    @GetMapping("/research_objects/{id}")
    public Resource<ResearchObject> one(@PathVariable String id) {
        ResearchObject researchObject = getResearchObject(id);

        return assembler.toResource(researchObject);
    }

    @DeleteMapping("/research_objects/{id}")
    public ResponseEntity<?> deleteResearchObject(@PathVariable String id) {
        ResearchObject researchObject = getResearchObject(id);

        researchObjectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profiles/{profileId}/research_objects")
    public ResponseEntity<Object> createResearchObject(@PathVariable String profileId, @RequestBody ResearchObject researchObject) {
        researchObject.setProfileId(profileId);
        ResearchObject savedResearchObject = researchObjectRepository.save(researchObject);

        Resource<ResearchObject> resource = assembler.toResource(savedResearchObject);

        return ResponseEntity.created(URI.create(resource.getLink("self").getHref())).body(resource);
    }

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

    @PostMapping(value="/research_objects/{id}/bundle", produces="application/zip")
    public void mintResearchObject(@PathVariable String id, HttpServletResponse response) throws Exception {
        ResearchObject researchObject = getResearchObject(id);

        ResearchObjectValidator validator = new ResearchObjectValidator();
        if (validator.validate(researchObject)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader("Content-Disposition", "attachment; filename=\"ro.bundle.zip\"");
            researchObject.bundle(response.getOutputStream());
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private ResearchObject getResearchObject(String id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }
}
