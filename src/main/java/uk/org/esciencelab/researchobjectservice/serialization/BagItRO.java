package uk.org.esciencelab.researchobjectservice.serialization;

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
    private long payloadOxumLength = 0;
    private long payloadOxumStreams = 0;

    private static final String [] defaultSupportedAlgorithms = { "MD5", "SHA-256", "SHA-512"};
    private static final Logger logger = LoggerFactory.getLogger(BagItROService.class);

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
        this.checksumMap = new HashMap<>(supportedAlgorithms.length);
        for (String alg : supportedAlgorithms) {
            this.checksumMap.put(alg, new HashMap<>());
        }
        this.remoteItems = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    /**
     * Write an actual file to the bag.
     * @param filename The file name to write to, under data/.
     * @param bytes The bytes of the file to write.
     */
    public void addData(String filename, byte [] bytes) throws NoSuchAlgorithmException, IOException {
        Path path = getDataFolder().resolve(filename);
        StreamWithDigests c = writeWithChecksums(new ByteArrayInputStream(bytes), Files.newOutputStream(path));
        updateChecksumMap(checksumMap, c, location.relativize(path));

        payloadOxumLength += bytes.length;
        payloadOxumStreams++;
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

        payloadOxumLength += entry.getLength();
        payloadOxumStreams++;
    }

    /**
     * Write out all the necessary tag files and manifests to the bag's location.
     */
    public Path write() throws NoSuchAlgorithmException, IOException {
        // Create a (unique) temp directory to hold the various BagIt files
        Path bagLocation = Files.createTempDirectory("bag");

        logger.info("Writing Bag to: " + bagLocation);

        Map<String, Map<Path, String>> tagChecksumMap = new HashMap<>(supportedAlgorithms.length);
        for (String alg : supportedAlgorithms) {
            tagChecksumMap.put(alg, new HashMap<>());
        }

        logger.info("Writing fetch.txt");
        writeFetch(tagChecksumMap);

        logger.info("Writing bag-info.txt");
        writeBagInfo(tagChecksumMap);

        logger.info("Writing bagit.txt");
        writeBagIt(tagChecksumMap);

        logger.info("Writing metadata/manifest.json");
        writeROManifest(tagChecksumMap);

        for (String alg : supportedAlgorithms) {
            logger.info("Writing manifest-" + alg.toLowerCase().replace("-", "") + ".txt");
            writeManifest(tagChecksumMap, alg);
        }

        for (String alg : supportedAlgorithms) {
            logger.info("Writing tagmanifest-" + alg.toLowerCase().replace("-", "") + ".txt");
            writeTagManifest(tagChecksumMap, alg);
        }

        return bagLocation;
    }

    private void updateChecksumMap(Map<String, Map<Path, String>> checksumMap, StreamWithDigests stream, Path path) {
        for (String alg : supportedAlgorithms) {
            checksumMap.get(alg).put(path, stream.getDigest(alg));
        }
    }

    private void writeFetch(Map<String, Map<Path, String>> tagChecksumMap) throws IOException, NoSuchAlgorithmException {
        Path path = location.resolve("fetch.txt");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), supportedAlgorithms);
        PrintStream s = new PrintStream(stream);

        for (RemoteResource entry : this.remoteItems) {
            s.println(entry.getUrl() + " " + entry.getLength() + " " + entry.getFilepath());
        }

        updateChecksumMap(tagChecksumMap, stream, path);
    }

    private void writeBagIt(Map<String, Map<Path, String>> tagChecksumMap) throws IOException, NoSuchAlgorithmException {
        Path path = location.resolve("bagit.txt");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), supportedAlgorithms);
        PrintStream s = new PrintStream(stream);

        s.println("BagIt-Version: 1.0");
        s.println("Tag-File-Character-Encoding: UTF-8");

        updateChecksumMap(tagChecksumMap, stream, path);
    }

    private void writeROManifest(Map<String, Map<Path, String>> tagChecksumMap) throws IOException, NoSuchAlgorithmException {
        Path roMetadataLocation = getMetadataFolder();
        Path path = roMetadataLocation.resolve("manifest.json");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), supportedAlgorithms);
        BagItROManifest roManifest = new BagItROManifest(roMetadataLocation);
        roManifest.setId(URI.create("../"));
        roManifest.setAggregates(remoteItems.stream().map(RemoteResource::getPathMetadata).collect(Collectors.toList()));
        roManifest.writeAsJsonLD(stream);

        updateChecksumMap(tagChecksumMap, stream, path);
    }

    private void writeBagInfo(Map<String, Map<Path, String>> tagChecksumMap) throws IOException, NoSuchAlgorithmException {
        Path path = location.resolve("bag-info.txt");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), supportedAlgorithms);
        PrintStream s = new PrintStream(stream);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String baggingDate = df.format(new Date());
        s.println("Bagging-Date: " + baggingDate);
        s.println("Payload-Oxum: " + payloadOxumLength + "." + payloadOxumStreams);

        updateChecksumMap(tagChecksumMap, stream, path);
    }

    private void writeManifest(Map<String, Map<Path, String>> tagChecksumMap, String algorithm) throws IOException, NoSuchAlgorithmException {
        Path path = location.resolve("manifest-" + algorithm.toLowerCase().replace("-", "") + ".txt");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), supportedAlgorithms);
        PrintStream s = new PrintStream(stream);

        for (Map.Entry<Path,String> entry : checksumMap.get(algorithm).entrySet()) {
            s.println(entry.getValue() + "  " + entry.getKey().toString());
        }

        updateChecksumMap(tagChecksumMap, stream, path);
    }

    private void writeTagManifest(Map<String, Map<Path, String>> tagChecksumMap, String algorithm) throws IOException, NoSuchAlgorithmException {
        Path path = location.resolve("tagmanifest-" + algorithm.toLowerCase().replace("-", "") + ".txt");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), supportedAlgorithms);
        PrintStream s = new PrintStream(stream);

        for (Map.Entry<Path,String> entry : tagChecksumMap.get(algorithm).entrySet()) {
            s.println(entry.getValue() + "  " + location.relativize(entry.getKey()).toString());
        }
    }

    private StreamWithDigests writeWithChecksums(InputStream in, OutputStream out) throws NoSuchAlgorithmException, IOException {
        StreamWithDigests output = new StreamWithDigests(out, supportedAlgorithms);

        int length;
        byte [] bytes = new byte[1024];
        while ((length = in.read(bytes)) >= 0) {
            output.write(bytes, 0, length);
        }

        return output;
    }

    private Path getDataFolder() throws IOException {
        return Files.createDirectories(location.resolve("data"));
    }

    private Path getMetadataFolder() throws IOException {
        return Files.createDirectories(location.resolve("metadata"));
    }
}
