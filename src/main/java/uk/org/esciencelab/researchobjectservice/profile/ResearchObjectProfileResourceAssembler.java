package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObjectController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ResearchObjectProfileResourceAssembler implements ResourceAssembler<ResearchObjectProfile, Resource<ResearchObjectProfile>> {

    @Override
    public Resource<ResearchObjectProfile> toResource(ResearchObjectProfile profile) {

        return new Resource<>(profile,
                linkTo(methodOn(ResearchObjectProfileController.class).one(profile.getId())).withSelfRel(),
                linkTo(methodOn(ResearchObjectController.class).allForProfile(profile.getId())).withRel("researchObjects"));
    }
}
