package uk.org.esciencelab.researchobjectservice.deposition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.net.URI;
import java.util.Map;

/**
 * A service to load the appropriate Depositor bean and perform the deposition action.
 */
@Service
public class DepositorService {
    @Autowired
    private ApplicationContext context;

    @Value("${depositor.bean}")
    private String defaultDepositorBeanName;

    /**
     * Deposit a Research Object in a remote repository.
     * @param researchObject The Research Object to deposit.
     * @param params A map of parameters to pass through to the depositor to use as required.
     * @return The URI of the deposited resource in the target repository.
     */
    public URI deposit(ResearchObject researchObject, Map<String, String> params) {
        URI depositionUri = getDefaultDepositor().deposit(researchObject, params);

        return depositionUri;
    }

    /**
     * Deposit a Research Object in a remote repository.
     * @param researchObject The Research Object to deposit.
     * @param depositorBeanName The name of the depositor bean to use.
     * @param params A map of parameters to pass through to the depositor to use as required.
     * @return The URI of the deposited resource in the target repository.
     */
    public URI deposit(ResearchObject researchObject, String depositorBeanName, Map<String, String> params) {
        URI depositionUri = getDepositor(depositorBeanName).deposit(researchObject, params);

        return depositionUri;
    }

    private Depositor getDefaultDepositor() {
        return (Depositor) context.getBean(defaultDepositorBeanName);
   }

    private Depositor getDepositor(String depositorBeanName) {
        return (Depositor) context.getBean(depositorBeanName);
   }
}
