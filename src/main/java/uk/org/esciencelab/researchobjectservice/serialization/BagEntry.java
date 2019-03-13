package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.apache.taverna.robundle.manifest.Proxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;

public class BagEntry {
    private Path bagRoot;
    private Path folder;
    private String filename;
    private URL url;
    private Long length;
    private HashMap<SupportedAlgorithm, String> checksums;

    public BagEntry(Path bagRoot, Path folder, String filename, URL url, Long length) {
        this.bagRoot = bagRoot;
        this.folder = folder;
        this.filename = filename;
        this.url = url;
        this.length = length;
        this.checksums = new HashMap<>(3);
    }

    public BagEntry(Path bagRoot, Path folder, JsonNode entryNode) throws MalformedURLException {
        this(bagRoot, folder,
                entryNode.get("filename").asText(),
                new URL(entryNode.get("url").asText()),
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
        return bagRoot.resolve(folder).resolve(getFilename());
    }

    public FetchItem getFetchItem() {
        return new FetchItem(url, length, getFilepath());
    }

    public PathMetadata getPathMetadata() {
        PathMetadata pm = new PathMetadata(url.toString());
        Proxy bundledAs = pm.getOrCreateBundledAs();
        bundledAs.setFilename(this.filename);
        bundledAs.setFolder(bagRoot.resolve("metadata").relativize(bagRoot.resolve(folder)));

        return pm;
    }

    public String toString() {
        return "[BagEntry: (" + filename + " @ " + url + " (" + length + ") " + bagRoot + "/" + folder + "]";
    }
}
