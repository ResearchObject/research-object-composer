package uk.org.esciencelab.researchobjectservice.serialization;


import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to wrap an OutputStream and compute a set of digests whilst writing.
 */
public class StreamWithDigests extends OutputStream {
    private OutputStream out;
    private String [] supportedAlgorithms;
    private Map<String, MessageDigest> digests;

    /**
     * Return the digest (as a hex string) for the given algorithm.
     * @param out The actual output stream being wrapped.
     * @param algorithms An array of algorithm names.
     */
    public StreamWithDigests(OutputStream out, String [] algorithms) throws NoSuchAlgorithmException {
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

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        for (String alg : supportedAlgorithms) {
            digests.get(alg).update(b);
        }
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    /**
     * Return the digest (as a hex string) for the given algorithm.
     * @param algorithm The name of the algorithm.
     */
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
