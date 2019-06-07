package uk.org.esciencelab.researchobjectservice;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileController;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObjectController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * A top-level endpoint that directs consumers to the RO and Profile sub-APIs.
 */
@Controller
class RootController {
    @GetMapping(value="/", produces = { "text/html" })
    public String ui() {
        return "index.html";
    }

    @GetMapping(value="/", produces = { "application/hal+json" })
    ResponseEntity<ResourceSupport> root() {
        ResourceSupport resourceSupport = new ResourceSupport();

        resourceSupport.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        resourceSupport.add(linkTo(methodOn(ResearchObjectProfileController.class).all()).withRel("profiles"));
        resourceSupport.add(linkTo(methodOn(ResearchObjectController.class).all(null, null)).withRel("researchObjects"));

        return ResponseEntity.ok(resourceSupport);
    }
}
