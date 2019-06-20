package uk.org.esciencelab.researchobjectservice.researchobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.org.esciencelab.researchobjectservice.deposition.DepositorService;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileNotFoundException;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileRepository;
import uk.org.esciencelab.researchobjectservice.serialization.BagItROService;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;

/**
 * A simple controller to handle viewing, listing, deleting, creating and bagging Research Objects.
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
    @Autowired
    private DepositorService depositorService;

    @GetMapping("/research_objects")
    public PagedResources<Resource<ResearchObjectSummary>> all(@PageableDefault Pageable pageable,
                                                               PagedResourcesAssembler pagedAssembler) {
        Page<ResearchObject> page =
                researchObjectRepository.findAll(pageable);

        return pagedAssembler.toResource(page, summaryAssembler);
    }

    @GetMapping("/research_objects/{id}")
    public Resource<ResearchObject> one(@PathVariable long id) {
        ResearchObject researchObject = getResearchObject(id);

        return assembler.toResource(researchObject);
    }

    @DeleteMapping("/research_objects/{id}")
    public ResponseEntity<?> deleteResearchObject(@PathVariable long id) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);

        researchObjectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profiles/{profileName}/research_objects")
    public Resources<Resource<ResearchObjectSummary>> allForProfile(@PathVariable String profileName,
                                                                    @PageableDefault Pageable pageable,
                                                                    PagedResourcesAssembler pagedAssembler) {
        ResearchObjectProfile profile = getResearchObjectProfile(profileName);

        Page<ResearchObject> page =
                researchObjectRepository.findAllByProfile(pageable, profile);

        return pagedAssembler.toResource(page, summaryAssembler);
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
        researchObject.validate();

        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"+" + id + "+.zip\"");
        bagItROService.bagToZip(researchObject, response.getOutputStream());
    }

    private ResearchObject getResearchObject(long id) {
        return researchObjectRepository.findById(id).orElseThrow(ResearchObjectNotFoundException::new);
    }

    @PostMapping(value="/research_objects/{id}/deposit", produces="text/plain")
    public String deposit(@PathVariable long id, HttpServletResponse response, @RequestParam Map<String,String> depositorParams) {
        ResearchObject researchObject = getResearchObject(id);
        checkMutable(researchObject);
        researchObject.validate();

        URI depositionUri = depositorService.deposit(researchObject, depositorParams);
        researchObject.setDepositionUrl(depositionUri);
        researchObjectRepository.save(researchObject);

        response.setStatus(HttpServletResponse.SC_OK);
        return depositionUri.toString();
    }

    private ResearchObjectProfile getResearchObjectProfile(String name) {
        return researchObjectProfileRepository.findByName(name).orElseThrow(ResearchObjectProfileNotFoundException::new);
    }

    private void checkMutable(ResearchObject researchObject) {
        if (!researchObject.isMutable()) {
            throw new ImmutableResearchObjectException();
        }
    }
}
