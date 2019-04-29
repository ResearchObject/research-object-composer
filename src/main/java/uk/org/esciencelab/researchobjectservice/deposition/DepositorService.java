package uk.org.esciencelab.researchobjectservice.deposition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.net.URI;

@PropertySource("classpath:depositor.properties")
@Service
public class DepositorService {
    @Autowired
    private ApplicationContext context;

    @Value("${depositor}")
    private String depositorBeanName;

    /**
     * Deposit a research object in a remote repository.
     * @param researchObject The research object to deposit.
     * @return The URI of the deposited resource in the target repository.
     */
    public URI deposit(ResearchObject researchObject) {
        URI depositionUri = getDepositor().deposit(researchObject);

        return depositionUri;
    }

    private Depositor getDepositor() {
        return (Depositor) context.getBean(depositorBeanName);
   }
}
