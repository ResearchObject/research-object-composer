package uk.org.esciencelab.researchobjectservice.deposition;

import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.net.URI;

public interface Depositor {
    public URI deposit(ResearchObject researchObject) throws DepositionException;
}
