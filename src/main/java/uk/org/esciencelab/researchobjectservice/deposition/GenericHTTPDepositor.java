package uk.org.esciencelab.researchobjectservice.deposition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;
import uk.org.esciencelab.researchobjectservice.serialization.BagItROService;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * An example implementation of a Depositor, that performs a simple HTTP POST of a zipped BagIt-RO serialization
 * of a Research Object to a configured URL, with some optional configured HTTP headers.
 */
@Component
public class GenericHTTPDepositor implements Depositor {

    @Autowired
    private GenericHTTPDepositorConfig config;

    private final String USER_AGENT = "Java/Research Object Composer";

    @Autowired
    private BagItROService bagItROService;

    public GenericHTTPDepositor() { }

    public URI deposit(ResearchObject researchObject) throws DepositionException {
        HttpURLConnection http;
        try {
            URL url = new URL(config.getUrl());
            if (url.getProtocol().equals("https")) {
                http = (HttpsURLConnection) url.openConnection();
            } else {
                http = (HttpURLConnection) url.openConnection();
            }

            http.setRequestMethod("POST");
            http.setDoOutput(true);
            // Apply headers
            Iterator<Map.Entry<String, String>> i = config.getHeaders().entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, String> headerPair = i.next();
                http.setRequestProperty(headerPair.getKey(), headerPair.getValue());
            }
            http.setRequestProperty("User-Agent", USER_AGENT);
            http.setRequestProperty("Content-Type", "application/zip");
            OutputStream os = http.getOutputStream();
            bagItROService.bagToZip(researchObject, os);
            http.connect();
            int code = http.getResponseCode();
            String loc = http.getHeaderField("Location");
            if (code == 200 || code == 201) {
                if (loc != null) {
                    return new URI(loc);
                } else {
                    throw new DepositionException("No Location header provided!");
                }
            } else {
                throw new DepositionException(code, null);
            }
        } catch (Exception e) {
            throw new DepositionException(e);
        }
    }
}
