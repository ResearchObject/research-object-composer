package uk.org.esciencelab.researchobjectservice.serialization;

import org.apache.commons.io.IOUtils;
import org.apache.jena.reasoner.rulesys.builtins.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class to construct BagItROs.
 */
public class BagItRO {

    private Path location;
    private String [] supportedAlgorithms;
    private List<RemoteResource> remoteItems;
    private List<File> files;
    private Map<String, Map<Path, String>> checksumMap;
    private long localByteLength = 0; // Total length of all local files in the bag
    private long localByteStreams = 0; // Total number of local files in the bag

    private static final String [] defaultSupportedAlgorithms = { "MD5", "SHA-256", "SHA-512"};

    /**
     *
     * @param location The location where to put the bag-in-progress (can be a temp directory!).
     */
    public BagItRO(Path location) {
        this(location, defaultSupportedAlgorithms);
    }

    /**
     *
     * @param location The location where to put the bag-in-progress.
     * @param checksumAlgorithms An array of checksum algorithm names to use in the manifest and tagmanifest files.
     */
    public BagItRO(Path location, String [] checksumAlgorithms) {
        this.location = location;
        this.supportedAlgorithms = checksumAlgorithms;
        this.checksumMap = getEmptyChecksumMap();
        this.remoteItems = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    /**
     * Write bytes to a file to the bag.
     * @param filename The file name to write to, under data/.
     * @param bytes The bytes of the file to write.
     */
    public void addData(String filename, byte [] bytes) throws NoSuchAlgorithmException, IOException {
        Path path = getDataFolder().resolve(filename);
        try (StreamWithDigests c = writeWithChecksums(new ByteArrayInputStream(bytes), Files.newOutputStream(path))) {

            updateChecksumMap(checksumMap, c, location.relativize(path));

            localByteLength += c.getLength();
            localByteStreams++;
            this.files.add(path.toFile());
        }
    }

    /**
     * Add a remote file to the bag.
     * @param entry The remote file to add, as a RemoteResource object.
     */
    public void addRemote(RemoteResource entry) {
        remoteItems.add(entry);

        for (String alg : supportedAlgorithms) {
            String sum = entry.getChecksum(alg);
            if (sum != null) {
                checksumMap.get(alg).put(entry.getFilepath(), sum);
            }
        }
    }

    /**
     * Write out all the necessary tag files and manifests to the bag's location.
     */
    public Path write() throws NoSuchAlgorithmException, IOException {
        // Create a (unique) temp directory to hold the various BagIt files
        new BagItROWriter(this).write();

        return location;
    }

    public Path getLocation() {
        return location;
    }

    public String[] getSupportedAlgorithms() {
        return supportedAlgorithms;
    }

    public List<RemoteResource> getRemoteItems() {
        return remoteItems;
    }

    public List<File> getFiles() {
        return files;
    }

    public Map<String, Map<Path, String>> getChecksumMap() {
        return checksumMap;
    }

    public String getPayloadOxum() {
        long payloadOxumLength = localByteLength;
        long payloadOxumStreams = localByteStreams;
        for (RemoteResource entry : this.getRemoteItems()) {
            payloadOxumLength += entry.getLength();
            payloadOxumStreams++;
        }

        return "" + payloadOxumLength + "." + payloadOxumStreams;
    }

    public Path getDataFolder() throws IOException {
        return Files.createDirectories(location.resolve("data"));
    }

    public Path getMetadataFolder() throws IOException {
        return Files.createDirectories(location.resolve("metadata"));
    }

    private void updateChecksumMap(Map<String, Map<Path, String>> checksumMap, StreamWithDigests stream, Path path) {
        for (String alg : supportedAlgorithms) {
            checksumMap.get(alg).put(path, stream.getDigest(alg));
        }
    }

    public Map<String, Map<Path, String>> getEmptyChecksumMap() {
        Map<String, Map<Path, String>> map = new HashMap<>(supportedAlgorithms.length);
        for (String alg : supportedAlgorithms) {
            map.put(alg, new HashMap<>());
        }

        return map;
    }

    private StreamWithDigests writeWithChecksums(InputStream in, OutputStream out) throws NoSuchAlgorithmException, IOException {
        StreamWithDigests output = new StreamWithDigests(out, supportedAlgorithms);

        IOUtils.copy(in, out);

        return output;
    }
}
