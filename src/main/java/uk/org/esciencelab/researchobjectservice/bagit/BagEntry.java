package uk.org.esciencelab.researchobjectservice.bagit;

import com.fasterxml.jackson.databind.JsonNode;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;

public class BagEntry {
    private Path basePath;
    private URL url;
    private String filename;
    private Long length;
    private HashMap<SupportedAlgorithm, String> checksums;

    public BagEntry(Path basePath, URL url, String filename, Long length) {
        this.basePath = basePath;
        this.url = url;
        this.filename = filename;
        this.length = length;
        this.checksums = new HashMap<>(3);
    }

    public BagEntry(Path basePath, JsonNode entryNode) throws MalformedURLException {
        this(basePath,
                new URL(entryNode.get("url").asText()),
                entryNode.get("filename").asText(),
                entryNode.get("length").asLong());

        for (JsonNode checksumNode : entryNode.get("checksums")) {
            if (checksumNode.get("type").asText().equals("md5")) {
                this.setChecksum(StandardSupportedAlgorithms.MD5, checksumNode.get("checksum").asText());
            } else if (checksumNode.get("type").asText().equals("sha256")) {
                this.setChecksum(StandardSupportedAlgorithms.SHA256, checksumNode.get("checksum").asText());
            } else if (checksumNode.get("type").asText().equals("sha512")) {
                this.setChecksum(StandardSupportedAlgorithms.SHA512, checksumNode.get("checksum").asText());
            }
        }
    }

    public URL getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public Long getLength() {
        return length;
    }

    public String getChecksum(SupportedAlgorithm alg) {
        return this.checksums.get(alg);
    }

    public void setChecksum(SupportedAlgorithm alg, String value) {
        this.checksums.put(alg, value);
    }

    public Path getFilepath() {
        return basePath.resolve(getFilename());
    }

    public FetchItem getFetchItem() {
        return new FetchItem(url, length, getFilepath());
    }

    public String toString() {
        return "[BagEntry: (" + filename + " @ " + url + " (" + length + ") " + basePath + "]";
    }
}
