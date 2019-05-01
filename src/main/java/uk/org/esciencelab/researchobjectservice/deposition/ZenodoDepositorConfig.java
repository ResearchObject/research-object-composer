package uk.org.esciencelab.researchobjectservice.deposition;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for a ZenodoDepositor
 */
@Component
@ConfigurationProperties(prefix = "depositor.zenodo")
public class ZenodoDepositorConfig {
    private String apiUrl;
    private String accessToken;

    /**
     * @return The base URL of the Zenodo instance to connect to (i.e. sandbox or production).
     */
    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * @return The client access token to authenticate with the Zenodo API.
     */
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
