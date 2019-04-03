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

@Component
public class GenericHTTPDepositor implements Depositor {

    @Autowired
    private GenericHTTPDepositorConfig config;

    private final String USER_AGENT = "Java/Research Object Composer";

    @Autowired
    private BagItROService bagItROService;

    public GenericHTTPDepositor() { }

    public URI deposit(ResearchObject researchObject) throws Exception {
        System.out.println(config.getUrl());
        URL url = new URL(config.getUrl());
        HttpURLConnection http;
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

        return new URI("hello://world");
    }
}
