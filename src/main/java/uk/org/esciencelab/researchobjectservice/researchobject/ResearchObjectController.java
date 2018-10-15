package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;
import uk.org.esciencelab.researchobjectservice.validator.ResearchObjectValidator;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@RestController
public class ResearchObjectController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;

    @Autowired
    private ResearchObjectProfileRepository researchObjectProfileRepository;

    @Autowired
    private ResearchObjectResourceAssembler assembler;

    @GetMapping(value="/profiles/{profileId}/research_objects", produces="application/hal+json")
    public Resources<Resource<ResearchObject>> allForProfile(@PathVariable String profileId) {
        ResearchObjectProfile profile = getProfile(profileId);
        List<ResearchObject> allByProfileId = researchObjectRepository.findAllByProfileId(profile.getId());
        bindProfiles(allByProfileId);
        List<Resource<ResearchObject>> researchObjectResources = allByProfileId.stream()
                .map(assembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(researchObjectResources,
                linkTo(methodOn(ResearchObjectController.class).allForProfile(profileId)).withSelfRel());
    }

    @GetMapping(value="/research_objects", produces="application/hal+json")
    public Resources<Resource<ResearchObject>> all() {
        List<ResearchObject> all = researchObjectRepository.findAll();
        bindProfiles(all);
        List<Resource<ResearchObject>> researchObjectResources = all.stream()
                .map(assembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(researchObjectResources,
                linkTo(methodOn(ResearchObjectController.class).all()).withSelfRel());
    }

    @GetMapping(value="/research_objects/{id}", produces="application/hal+json")
    public Resource<ResearchObject> one(@PathVariable String id) {
        ResearchObject researchObject = researchObjectRepository.findById(id).get();

        return assembler.toResource(researchObject);
    }

    @DeleteMapping("/research_objects/{id}")
    public void deleteResearchObject(@PathVariable String id) {
        researchObjectRepository.deleteById(id);
    }

    @PostMapping("/profiles/{profileId}/research_objects")
    public ResponseEntity<Object> createResearchObject(@PathVariable String profileId, @RequestBody ResearchObject researchObject) {
        researchObject.setProfile(getProfile(profileId));
        ResearchObject savedResearchObject = researchObjectRepository.save(researchObject);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(savedResearchObject.getId()).toUri();

        return ResponseEntity.created(location).build();
    }
//
//    @PutMapping("/research_objects/{id}")
//    public ResponseEntity<Object> updateResearchObject(@PathVariable String id, @RequestBody ResearchObject researchObject) {
//
//        Optional<ResearchObject> researchObjectOptional = researchObjectRepository.findById(id);
//
//        if (!researchObjectOptional.isPresent())
//            return ResponseEntity.notFound().build();
//
//        researchObject.setId(id);
//
//        researchObjectRepository.save(researchObject);
//
//        Resource<ResearchObject> resource = assembler.toResource(researchObject);
//
//        return ResponseEntity.ok().body(resource);
//    }

    @GetMapping("/research_objects/{id}/{field}")
    public ResponseEntity<Object> getResearchObjectField(@PathVariable String id, @PathVariable String field) {
        Optional<ResearchObject> researchObjectOptional = researchObjectRepository.findById(id);

        if (!researchObjectOptional.isPresent())
            return ResponseEntity.notFound().build();

        ResearchObject researchObject = researchObjectOptional.get();
        researchObject.setProfile(researchObjectProfileRepository.findById(researchObject.getProfileId()).get());

        return ResponseEntity.ok(researchObject.getField(field));
    }

    @PostMapping(value="/research_objects/{id}/bundle", produces="application/zip")
    public void mintResearchObject(@PathVariable String id, HttpServletResponse response) throws Exception {
        Optional<ResearchObject> researchObjectOptional = researchObjectRepository.findById(id);

        if (!researchObjectOptional.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            ResearchObject researchObject = researchObjectOptional.get();
            ResearchObjectValidator validator = new ResearchObjectValidator();
            if (validator.validate(researchObject)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.addHeader("Content-Disposition", "attachment; filename=\"ro.bundle.zip\"");
                researchObjectOptional.get().bundle(response.getOutputStream());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    @PutMapping("/research_objects/{id}/{field}")
    public ResponseEntity<Object> updateResearchObjectField(@PathVariable String id, @PathVariable String field, @RequestBody String value) throws Exception {
        Optional<ResearchObject> researchObjectOptional = researchObjectRepository.findById(id);

        if (!researchObjectOptional.isPresent())
            return ResponseEntity.notFound().build();

        ResearchObject researchObject = researchObjectOptional.get();
        researchObject.setProfile(researchObjectProfileRepository.findById(researchObject.getProfileId()).get());
        researchObject.setField(field, value);

        researchObjectRepository.save(researchObject);

        return ResponseEntity.noContent().build();
    }

    private ResearchObjectProfile getProfile(String profileId) {
        Optional<ResearchObjectProfile> profile = researchObjectProfileRepository.findById(profileId);

        return profile.get();
    }

    private void bindProfiles(List<ResearchObject> researchObjects) {
        Iterator<ResearchObject> i = researchObjects.iterator();
        while (i.hasNext()) {
            ResearchObject researchObject = i.next();
            researchObject.setProfile(getProfile(researchObject.getProfileId()));
        }
    }
}
