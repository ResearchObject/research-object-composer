package uk.org.esciencelab.researchobjectservice.serialization;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to construct BagIt bags.
 */
public class Bag {

    private Path location;
    private String [] supportedAlgorithms;
    private List<RemoteResource> remoteItems;
    private List<File> files;
    private Map<String, Map<Path, String>> fileChecksums;
    private long localByteLength = 0; // Total length of all local files in the bag
    private long localByteStreams = 0; // Total number of local files in the bag
    private static final String [] defaultSupportedAlgorithms = { "MD5", "SHA-256", "SHA-512"};

    /**
     * @param location The location where to put the bag-in-progress (can be a temp directory!).
     */
    public Bag(Path location) {
        this(location, defaultSupportedAlgorithms);
    }

    /**
     * @param location The location where to put the bag-in-progress.
     * @param checksumAlgorithms An array of checksum algorithm names to use in the manifest and tagmanifest files.
     */
    public Bag(Path location, String [] checksumAlgorithms) {
        this.location = location;
        this.supportedAlgorithms = checksumAlgorithms;
        this.fileChecksums = getEmptyChecksumMap();
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

            updateChecksumMap(fileChecksums, c, location.relativize(path));

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
                fileChecksums.get(alg).put(entry.getFilepath(), sum);
            }
        }
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

    public Map<String, Map<Path, String>> getFileChecksums() {
        return fileChecksums;
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

    private void updateChecksumMap(Map<String, Map<Path, String>> fileChecksums, StreamWithDigests stream, Path path) {
        for (String alg : supportedAlgorithms) {
            fileChecksums.get(alg).put(path, stream.getDigest(alg));
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
