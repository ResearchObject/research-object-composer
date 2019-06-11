package uk.org.esciencelab.researchobjectservice.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.Link;

/*
 HATEOAS's Link class does not support the "name" attribute yet, so we are using the "title" attribute as the "name".
 */
public class LinkWithName extends Link {

    private String name;

    public LinkWithName(String href, String rel, String name) {
        super(href, rel);
        this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
        return this.name;
    }
}
