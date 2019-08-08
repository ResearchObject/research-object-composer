package uk.org.esciencelab.researchobjectservice.serialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

/**
 * A class for writing all necessary tag files etc. for a given Bag as a BagIt RO, with an RO manifest.
 */
public class BagItROWriter extends BagWriter {
    private static final Logger logger = LoggerFactory.getLogger(BagItROWriter.class);

    /**
     * Create a new BagIt RO writer for the given bag.
     * @param bag The bag to write.
     */
    public BagItROWriter(Bag bag) {
        super(bag);
    }

    @Override
    protected void writeTagFiles() throws IOException, NoSuchAlgorithmException {
        writeROManifest();
        super.writeTagFiles();
    }

    private void writeROManifest() throws IOException, NoSuchAlgorithmException {
        logger.info("Writing metadata/manifest.json");
        Path roMetadataLocation = Files.createDirectories(bag.getLocation().resolve("metadata"));
        Path path = roMetadataLocation.resolve("manifest.json");
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), bag.getSupportedAlgorithms());
        BagItROManifest roManifest = new BagItROManifest(roMetadataLocation);
        roManifest.setId(URI.create("../"));
        roManifest.setAggregates(bag.getRemoteItems().stream().map(RemoteResource::getPathMetadata).collect(Collectors.toList()));
        roManifest.writeAsJsonLD(stream);

        updateChecksumMap(tagFileChecksums, stream, path);
    }
}
