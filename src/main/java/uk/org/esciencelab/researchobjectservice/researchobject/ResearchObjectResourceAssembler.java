package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfileController;

import java.util.Iterator;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * An assembler to control how a research object is serialized as a HAL+JSON document.
 */
@Component
public class ResearchObjectResourceAssembler implements ResourceAssembler<ResearchObject, Resource<ResearchObject>> {

    /**
     * Create a research object "resource" which consists of a serialized ResearchObject bean, and various links where
     * additional requests can be performed, including a link to the profile, a link to get the content as plain JSON in isolation,
     * and links to each top-level field in the content.
     * @param researchObject
     * @return
     */
    @Override
    public Resource<ResearchObject> toResource(ResearchObject researchObject) {
        Resource<ResearchObject> resource = new Resource<>(researchObject,
                linkTo(methodOn(ResearchObjectController.class).one(researchObject.getId())).withSelfRel(),
                linkTo(methodOn(ResearchObjectProfileController.class).one(researchObject.getProfileName())).withRel("profile"));

        resource.add(linkTo(methodOn(ContentController.class).getResearchObjectContent(researchObject.getId())).withRel("content"));
        Iterator<String> i = researchObject.getContent().fieldNames();
        while(i.hasNext()) {
            String field = i.next();
            resource.add(linkTo(methodOn(ContentController.class).getResearchObjectField(researchObject.getId(), field)).withRel(field));
        }

        return resource;
    }
}

