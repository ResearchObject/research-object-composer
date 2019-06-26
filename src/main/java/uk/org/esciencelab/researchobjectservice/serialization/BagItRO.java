package uk.org.esciencelab.researchobjectservice.serialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BagItRO {

    private Path location;
    private List<BagEntry> remoteItems;
    private List<File> files;
    private Map<String, Map<Path, String>> checksumMap;
    private static final String [] supportedAlgorithms = { "MD5", "SHA-256", "SHA-512"};

    public BagItRO(Path location) {
        this.location = location;
        this.checksumMap = new HashMap<>(3);
        for (String alg : supportedAlgorithms) {
            this.checksumMap.put(alg, new HashMap<>());
        }
        this.remoteItems = new ArrayList<BagEntry>();
        this.files = new ArrayList<File>();
    }

    public void addData(Path path, byte [] bytes) throws NoSuchAlgorithmException, IOException {
        for (String alg : supportedAlgorithms) {
            MessageDigest digest = MessageDigest.getInstance(alg);

            checksumMap.get(alg).put(path, bytesToHex(digest.digest(bytes)));
        }

        Files.write(location.resolve(path), bytes);
    }

    public void addRemote(BagEntry entry) {
        remoteItems.add(entry);

        for (String alg : supportedAlgorithms) {
            String sum = entry.getChecksum(alg);
            if (sum != null) {
                checksumMap.get(alg).put(entry.getFilepath(), sum);
            }
        }
    }

    public void write() {

    }

    private static String bytesToHex(byte [] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
