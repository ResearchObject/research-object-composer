package uk.org.esciencelab.researchobjectservice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * A controller to serve the static HTML pages
 */
@Controller
class StaticController {
    @GetMapping(value="/", produces = { "text/html" })
    public String ui() {
        return "index.html";
    }
}
