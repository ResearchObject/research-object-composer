package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileController;

import java.util.Iterator;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ResearchObjectResourceAssembler implements ResourceAssembler<ResearchObject, Resource<ResearchObject>> {

    @Override
    public Resource<ResearchObject> toResource(ResearchObject researchObject) {
        Resource<ResearchObject> resource = new Resource<>(researchObject,
                linkTo(methodOn(ResearchObjectController.class).one(researchObject.getId())).withSelfRel(),
                linkTo(methodOn(ResearchObjectProfileController.class).one(researchObject.getProfileName())).withRel("profile"));

        Iterator<String> i = researchObject.getContent().fieldNames();
        while(i.hasNext()) {
            String field = i.next();
            resource.add(linkTo(methodOn(FieldController.class).getResearchObjectField(researchObject.getId(), field)).withRel(field));
        }

        return resource;
    }
}

