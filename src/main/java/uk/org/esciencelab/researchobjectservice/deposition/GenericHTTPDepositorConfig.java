package uk.org.esciencelab.researchobjectservice.deposition;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration for a GenericHTTPDepositor
 */
@Component
@ConfigurationProperties(prefix = "depositor.http")
public class GenericHTTPDepositorConfig {
    private String url;
    private Map<String, String> headers;

    /**
     * @return The URL to POST to.
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return The HTTP headers to set on the request.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
