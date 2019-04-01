package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileNotFoundException;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;
import uk.org.esciencelab.researchobjectservice.serialization.BagItROService;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * A simple controller to handle viewing, listing, deleting, creating and bagging research objects.
 */
@RestController
public class ResearchObjectController {
    @Autowired
    private ResearchObjectRepository researchObjectRepository;
    @Autowired
    private ResearchObjectProfileRepository researchObjectProfileRepository;
    @Autowired
    private ResearchObjectResourceAssembler assembler;
    @Autowired
    private ResearchObjectSummaryResourceAssembler summaryAssembler;
    @Autowired
    private BagItROService bagItROService;

    @GetMapping("/research_objects")
    public Resources<Resource<ResearchObjectSummary>> all() {
        Collection<ResearchObject> all = (Collection) researchObjectRepository.findAll();
        List<Resource<ResearchObjectSummary>> researchObjectResources = all.stream()
                .map(summaryAssembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(researchObjectResources,
                linkTo(methodOn(ResearchObjectController.class).all()).withSelfRel());
    }

    @GetMapping("/research_objects/{id}")
    public Resource<ResearchObject> one(@PathVariable long id) {
        ResearchObject researchObject = getResearchObject(id);

        return assembler.toResource(researchObject);
    }

    @DeleteMapping("/research_objects/{id}")
    public ResponseEntity<?> deleteResearchObject(@PathVariable long id) {
        getResearchObject(id); // This is here to check the RO exists, throwing a 404 otherwise.

        researchObjectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profiles/{profileName}/research_objects")
    public Resources<Resource<ResearchObjectSummary>> allForProfile(@PathVariable String profileName) {
        ResearchObjectProfile profile = getResearchObjectProfile(profileName);
        List<ResearchObject> allByProfile = researchObjectRepository.findAllByProfile(profile);
        List<Resource<ResearchObjectSummary>> researchObjectResources = allByProfile.stream()
                .map(summaryAssembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(researchObjectResources,
                linkTo(methodOn(ResearchObjectController.class).allForProfile(profileName)).withSelfRel());
    }

    @PostMapping("/profiles/{profileName}/research_objects")
    public ResponseEntity<Object> createResearchObject(@PathVariable String profileName, @RequestBody(required=false) JsonNode content) {
        ResearchObject researchObject = new ResearchObject(getResearchObjectProfile(profileName));

        if (content != null) {
            researchObject.setAndValidateContent((ObjectNode) content);
        }

        ResearchObject savedResearchObject = researchObjectRepository.save(researchObject);
        Resource<ResearchObject> resource = assembler.toResource(savedResearchObject);
        return ResponseEntity.created(URI.create(resource.getLink("self").getHref())).body(resource);
    }

    @PostMapping(value="/research_objects/{id}/bag", produces="application/zip")
    public void mintBag(@PathVariable long id, HttpServletResponse response) throws Exception {
        ResearchObject researchObject = getResearchObject(id);

        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"+" + id + "+.zip\"");
        bagItROService.bagToZip(researchObject, response.getOutputStream());
    }

    private ResearchObject getResearchObject(long id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }

    private ResearchObjectProfile getResearchObjectProfile(String name) {
        return researchObjectProfileRepository.findByName(name).orElseThrow(ResearchObjectProfileNotFoundException::new);
    }
}
