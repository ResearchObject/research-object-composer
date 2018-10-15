package uk.org.esciencelab.researchobjectservice.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@RestController
public class ResearchObjectProfileController {
    @Autowired
    private ResearchObjectProfileRepository researchObjectProfileRepository;
    @Autowired
    private ResearchObjectProfileResourceAssembler assembler;

    @GetMapping(value="/profiles", produces="application/hal+json")
    public Resources<Resource<ResearchObjectProfile>> all() {
        List<Resource<ResearchObjectProfile>> profiles = researchObjectProfileRepository.findAll().stream()
                .map(assembler::toResource)
                .collect(Collectors.toList());

        return new Resources<>(profiles,
                linkTo(methodOn(ResearchObjectProfileController.class).all()).withSelfRel());
    }

    @GetMapping(value="/profiles/{id}", produces="application/hal+json")
    public Resource<ResearchObjectProfile> one(@PathVariable String id) {
        ResearchObjectProfile profile = researchObjectProfileRepository.findById(id).get();

        return assembler.toResource(profile);
    }
}
