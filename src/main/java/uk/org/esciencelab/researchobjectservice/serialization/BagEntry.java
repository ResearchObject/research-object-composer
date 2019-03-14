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

/**
 * A remote resource to be bagged.
 */
public class BagEntry {
    private Path bagRoot;
    private Path folder;
    private String filename;
    private URL url;
    private Long length;
    private HashMap<SupportedAlgorithm, String> checksums;

    /**
     *
     * @param bagRoot A path to the root of the bag.
     * @param folder A relative path within the bag where this resource will be bagged. Should start with "data".
     * @param filename The filename of this resource.
     * @param url The URL from which this resource should be fetched/
     * @param length The length in bytes of the resource.
     */
    public BagEntry(Path bagRoot, Path folder, String filename, URL url, Long length) {
        this.bagRoot = bagRoot;
        this.folder = folder;
        this.filename = filename;
        this.url = url;
        this.length = length;
        this.checksums = new HashMap<>(3);
    }

    /**
     *
     * @param bagRoot A path to the root of the bag.
     * @param folder A relative path within the bag where this resource will be bagged. Should start with "data".
     * @param entryNode A JSON object conforming to `/schemas/_base.schema.json#/definitions/RemoteItem`, containing
     *                  information on the filename, URL, length and checksums of this resource.
     * @throws MalformedURLException
     */
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

    /**
     * Create a FetchItem entry for this resource, used to populate fetch.txt in the BagIt bag.
     * @return The FetchItem entry.
     */
    public FetchItem getFetchItem() {
        return new FetchItem(url, length, getFilepath());
    }

    /**
     * Create a PathMetadata entry for this resource, used to populate the "aggregates" section of the RO's manifest.json.
     * @return The PathMetadata entry.
     */
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
