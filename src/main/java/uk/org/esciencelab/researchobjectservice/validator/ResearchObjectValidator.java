package uk.org.esciencelab.researchobjectservice.validator;

import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import uk.org.esciencelab.researchobjectservice.profile.ResearchObjectProfile;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.util.ArrayList;

public class ResearchObjectValidator {

    private JSONObject errors;

    public ResearchObjectValidator() { }

    public boolean validate(ResearchObject researchObject) {
        ResearchObjectProfile profile = researchObject.getProfile();

        try {
            profile.getSchema().validate(new JSONObject(researchObject.getFields()));
        } catch (ValidationException e) {
            this.errors = e.toJSON();
            System.out.println(getErrors());

            return false;
        }

        return true;
    }

    public JSONObject getErrors() {
        return errors;
    }
}
