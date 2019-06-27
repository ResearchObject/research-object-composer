package uk.org.esciencelab.researchobjectservice.serialization;


import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class ChecksummedOutputStream extends OutputStream {
    private OutputStream out;
    private String [] supportedAlgorithms;
    private Map<String, MessageDigest> digests;

    public ChecksummedOutputStream(OutputStream out, String [] algorithms) throws NoSuchAlgorithmException {
        this.out = out;
        this.supportedAlgorithms = algorithms;
        this.digests = new HashMap<>(algorithms.length);
        for (String alg : algorithms) {
            MessageDigest digest = MessageDigest.getInstance(alg);
            digests.put(alg, digest);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        for (String alg : supportedAlgorithms) {
            digests.get(alg).update(b, off, len);
        }
    }

    @Override
    public void write(int i) throws IOException {
        out.write(i);
        for (String alg : supportedAlgorithms) {
            digests.get(alg).update((byte) i);
        }
    }

    public String getDigest(String algorithm) {
        return bytesToHex(digests.get(algorithm).digest());
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
