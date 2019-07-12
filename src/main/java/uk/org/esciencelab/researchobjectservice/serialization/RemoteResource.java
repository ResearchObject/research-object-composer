package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.apache.taverna.robundle.manifest.Proxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * A remote resource to be bagged.
 */
public class RemoteResource {
    private Path folder;
    private String filename;
    private URL url;
    private long length;
    private HashMap<String, String> checksums;
    private static Path root = Paths.get("/");

    /**
     *
     * @param folder A relative path where this resource will be bagged (within `_bag_root_/data`).
     * @param filename The filename of this resource.
     * @param url The URL from which this resource should be fetched/
     * @param length The length in bytes of the resource.
     */
    public RemoteResource(Path folder, String filename, URL url, long length) {
        this.folder = folder;
        this.filename = filename;
        this.url = url;
        this.length = length;
        this.checksums = new HashMap<>(3);
    }

    /**
     *
     * @param folder A string containing an absolute path of a folder in which to bag this resource. The root ("/") of
     *               the path is `_bag_root_/data`, so `/foo` would be bagged at `_bag_root_/data/foo` and `/` would be
     *               bagged at `_bag_root_/data`.
     * @param entryNode A JSON object conforming to `/schemas/_base.schema.json#/definitions/RemoteItem`, containing
     *                  information on the filename, URL, length and checksums of this resource.
     * @throws MalformedURLException
     */
    public RemoteResource(String folder, JsonNode entryNode) throws MalformedURLException {
        this(Paths.get("/").relativize(Paths.get(folder)),
                entryNode.get("filename").asText(),
                new URL(entryNode.get("url").asText()),
                entryNode.get("length").asLong());

        for (JsonNode checksumNode : entryNode.get("checksums")) {
            if (checksumNode.get("type").asText().equals("md5")) {
                this.setChecksum("MD5", checksumNode.get("checksum").asText());
            } else if (checksumNode.get("type").asText().equals("sha256")) {
                this.setChecksum("SHA-256", checksumNode.get("checksum").asText());
            } else if (checksumNode.get("type").asText().equals("sha512")) {
                this.setChecksum("SHA-512", checksumNode.get("checksum").asText());
            }
        }
    }

    public URL getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public long getLength() {
        return length;
    }

    public String getChecksum(String alg) {
        return this.checksums.get(alg);
    }

    public void setChecksum(String alg, String value) {
        this.checksums.put(alg, value);
    }

    public Path getFullFolderPath() {
        return root.resolve("data").resolve(folder);
    }

    public Path getFilepath() {
        return root.relativize(getFullFolderPath().resolve(filename));
    }

    /**
     * Create a PathMetadata entry for this resource, used to populate the "aggregates" section of the RO's manifest.json.
     * @return The PathMetadata entry.
     */
    public PathMetadata getPathMetadata() {
        PathMetadata pm = new PathMetadata(url.toString());
        Proxy bundledAs = pm.getOrCreateBundledAs();
        bundledAs.setFilename(this.filename);
        // Folder path should be relative to the `_bag_root_/metadata` directory (e.g. `../data/foo`).
        bundledAs.setFolder(root.resolve("metadata").relativize(getFullFolderPath()));

        return pm;
    }

    public String toString() {
        return "[RemoteResource: (" + filename + " @ " + url + " (" + length + ") " + getFilepath() + "]";
    }
}
