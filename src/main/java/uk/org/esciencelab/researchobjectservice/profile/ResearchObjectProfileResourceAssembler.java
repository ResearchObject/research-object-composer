package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObjectController;

import static org.springframework.hateoas.mvc.BasicLinkBuilder.linkToCurrentMapping;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * An assembler to control how the profile is serialized as a HAL+JSON document.
 */
@Component
public class ResearchObjectProfileResourceAssembler implements ResourceAssembler<ResearchObjectProfile, Resource<ResearchObjectProfile>> {

    /**
     * Create a RO profile "resource" which consists of a serialized ResearchObjectProfile bean, and various links where
     * additional requests can be performed.
     * @param profile
     * @return
     */
    @Override
    public Resource<ResearchObjectProfile> toResource(ResearchObjectProfile profile) {
        return new Resource<>(profile,
                linkTo(methodOn(ResearchObjectProfileController.class).one(profile.getName())).withSelfRel(),
                linkToCurrentMapping().slash(profile.getSchemaPath()).withRel("schema"),
                linkTo(methodOn(ResearchObjectProfileController.class).template(profile.getName())).withRel("template"),
                linkTo(methodOn(ResearchObjectController.class).allForProfile(profile.getName(),null, null)).withRel("researchObjects"));
    }
}
