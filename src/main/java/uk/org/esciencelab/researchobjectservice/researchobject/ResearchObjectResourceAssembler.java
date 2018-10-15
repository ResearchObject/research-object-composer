package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileController;

import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ResearchObjectResourceAssembler implements ResourceAssembler<ResearchObject, Resource<ResearchObject>> {

    @Override
    public Resource<ResearchObject> toResource(ResearchObject researchObject) {
        Resource<ResearchObject> resource = new Resource<>(researchObject,
                linkTo(methodOn(ResearchObjectController.class).one(researchObject.getId())).withSelfRel(),
                linkTo(methodOn(ResearchObjectProfileController.class).one(researchObject.getProfileId())).withRel("profile"));

        for (Map.Entry<String, Object> entry : researchObject.getFields().entrySet()) {
            resource.add(linkTo(methodOn(ResearchObjectController.class).getResearchObjectField(researchObject.getId(), entry.getKey())).withRel(entry.getKey()));
        }

        return resource;
    }
}

