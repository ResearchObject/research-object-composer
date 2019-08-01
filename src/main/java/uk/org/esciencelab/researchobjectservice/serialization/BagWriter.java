package uk.org.esciencelab.researchobjectservice.serialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * A class for writing all necessary tag files etc. for a given Bag as a plain BagIt bag.
 */
public class BagWriter {

    class TagFileWriter {
        private Path path;
        private StreamWithDigests stream;
        private PrintStream printStream;

        public TagFileWriter(Path path) throws IOException, NoSuchAlgorithmException {
            this.stream = new StreamWithDigests(Files.newOutputStream(path), bag.getSupportedAlgorithms());
            this.path = path;
            this.printStream = new PrintStream(stream, true);
        }

        public void println(String str) {
            printStream.println(str);
        }

        public void close() {
            printStream.close();
            updateChecksumMap(tagFileChecksums, stream, path);
        }
    }

    protected Bag bag;
    protected Map<String, Map<Path, String>> tagFileChecksums;
    private static final Logger logger = LoggerFactory.getLogger(BagWriter.class);

    public BagWriter(Bag bag) {
        this.bag = bag;
        this.tagFileChecksums = bag.getEmptyChecksumMap();
    }
    
    public void write() throws IOException, NoSuchAlgorithmException {
        logger.info("Writing Bag to: " + bag.getLocation());
        writeTagFiles();
        for (String alg : bag.getSupportedAlgorithms()) {
            writeTagManifest(alg);
        }
    }

    protected void writeTagFiles() throws IOException, NoSuchAlgorithmException {
        writeFetch();
        writeBagInfo();
        writeBagIt();
        for (String alg : bag.getSupportedAlgorithms()) {
            writeManifest(alg);
        }
    }

    protected void writeFetch() throws IOException, NoSuchAlgorithmException {
        logger.info("Writing fetch.txt");
        TagFileWriter s = new TagFileWriter(bag.getLocation().resolve("fetch.txt"));
        try {
            for (RemoteResource entry : this.bag.getRemoteItems()) {
                s.println(entry.getUrl() + " " + entry.getLength() + " " + entry.getFilepath());
            }
        } finally {
            s.close();
        }
    }

    protected void writeBagIt() throws IOException, NoSuchAlgorithmException {
        logger.info("Writing bagit.txt");
        TagFileWriter s = new TagFileWriter(bag.getLocation().resolve("bagit.txt"));
        try {
            s.println("BagIt-Version: 1.0");
            s.println("Tag-File-Character-Encoding: UTF-8");
        } finally {
            s.close();
        }
    }

    protected void writeBagInfo() throws IOException, NoSuchAlgorithmException {
        logger.info("Writing bag-info.txt");
        TagFileWriter s = new TagFileWriter(bag.getLocation().resolve("bag-info.txt"));
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String baggingDate = df.format(new Date());
            s.println("Bagging-Date: " + baggingDate);
            s.println("Payload-Oxum: " + bag.getPayloadOxum());
        } finally {
            s.close();
        }
    }

    protected void writeManifest(String algorithm) throws IOException, NoSuchAlgorithmException {
        Path path = bag.getLocation().resolve("manifest-" + algorithm.toLowerCase().replace("-", "") + ".txt");
        logger.info("Writing " + bag.getLocation().relativize(path).toString());
        TagFileWriter s = new TagFileWriter(path);
        try {
            for (Map.Entry<Path,String> entry : bag.getFileChecksums().get(algorithm).entrySet()) {
                s.println(entry.getValue() + "  " + entry.getKey().toString());
            }
        } finally {
            s.close();
        }
    }

    protected void writeTagManifest(String algorithm) throws IOException, NoSuchAlgorithmException {
        Path path = bag.getLocation().resolve("tagmanifest-" + algorithm.toLowerCase().replace("-", "") + ".txt");
        logger.info("Writing " + bag.getLocation().relativize(path).toString());
        StreamWithDigests stream = new StreamWithDigests(Files.newOutputStream(path), bag.getSupportedAlgorithms());
        PrintStream s = new PrintStream(stream);

        for (Map.Entry<Path,String> entry : tagFileChecksums.get(algorithm).entrySet()) {
            s.println(entry.getValue() + "  " + bag.getLocation().relativize(entry.getKey()).toString());
        }
    }

    protected void updateChecksumMap(Map<String, Map<Path, String>> checksumMap, StreamWithDigests stream, Path path) {
        for (String alg : bag.getSupportedAlgorithms()) {
            checksumMap.get(alg).put(path, stream.getDigest(alg));
        }
    }
}
