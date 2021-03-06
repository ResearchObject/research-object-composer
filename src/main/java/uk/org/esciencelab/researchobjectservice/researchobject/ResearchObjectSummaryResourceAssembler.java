package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * An assembler to serialize a Research Object in a minimal "summary" HAL+JSON document.
 */
@Component
public class ResearchObjectSummaryResourceAssembler implements ResourceAssembler<ResearchObject, Resource<ResearchObjectSummary>> {

    @Override
    public Resource<ResearchObjectSummary> toResource(ResearchObject researchObject) {
        ResearchObjectSummary summary = new ResearchObjectSummary(researchObject);
        Resource<ResearchObjectSummary> resource = new Resource<>(summary,
                linkTo(methodOn(ResearchObjectController.class).one(researchObject.getId())).withSelfRel(),
                linkTo(methodOn(ResearchObjectProfileController.class).one(researchObject.getProfileName())).withRel("profile"));

        if (researchObject.getDepositionUrl() != null)
            resource.add(new Link(researchObject.getDepositionUrl().toString(), "deposition"));

        return resource;
    }
}

