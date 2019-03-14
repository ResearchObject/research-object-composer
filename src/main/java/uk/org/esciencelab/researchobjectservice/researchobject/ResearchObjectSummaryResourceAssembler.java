package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * An assembler to serialize a research object in a minimal "summary" HAL+JSON document.
 */
@Component
public class ResearchObjectSummaryResourceAssembler implements ResourceAssembler<ResearchObject, Resource<ResearchObjectSummary>> {

    @Override
    public Resource<ResearchObjectSummary> toResource(ResearchObject researchObject) {
        ResearchObjectSummary summary = new ResearchObjectSummary(researchObject);
        Resource<ResearchObjectSummary> resource = new Resource<>(summary,
                linkTo(methodOn(ResearchObjectController.class).one(researchObject.getId())).withSelfRel());

        return resource;
    }
}

