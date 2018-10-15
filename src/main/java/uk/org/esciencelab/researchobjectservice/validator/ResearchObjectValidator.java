package uk.org.esciencelab.researchobjectservice.validator;

import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.util.List;

public class ResearchObjectValidator {

    private List<String> errors;

    public ResearchObjectValidator() { }

    public boolean validate(ResearchObject researchObject) {
        return true;
    }

    public List<String> getErrors() {
        return errors;
    }
}
