package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import org.apache.taverna.robundle.manifest.Manifest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.nio.file.Files.createDirectories;

/**
 * A modification of the ROBundle Manifest "bean", that allows writing the JSON-LD manifest to the appropriate
 * directory in accordance with https://w3id.org/ro/bagit
 */
public class BagItROManifest extends Manifest {
    private static final String MANIFEST_JSON = "manifest.json";

    private Path root;

    /**
     * @param root An absolute path to the root of the RO metadata folder within the BagIt bag.
     *             Should be something like "/tmp/bag1251515151/metadata"
     */
    public BagItROManifest(Path root) {
        super(null);
        this.root = root;
    }

    @Override
    public Path writeAsJsonLD() throws IOException {
        OutputStream o = Files.newOutputStream(this.root.resolve(MANIFEST_JSON));

        return this.writeAsJsonLD(o);
    }

    /**
     * Write the manifest to the given output stream.
     * @param out The stream to write to.
     * @return The path to the manifest file.
     */
    public Path writeAsJsonLD(OutputStream out) throws IOException {
        Path jsonld = this.root.resolve(MANIFEST_JSON);
        createDirectories(jsonld.getParent());
        if (!getManifest().contains(jsonld))
            getManifest().add(0, this.root.relativize(jsonld));

        // This is to fix a bug where folders do not have trailing slashes
        SerializerFactory serializerFactory = BeanSerializerFactory.instance
                .withSerializerModifier(new ProxySerializerModifier());

        ObjectMapper om = new ObjectMapper()
                .addMixIn(Path.class, PathMixin.class)
                .addMixIn(FileTime.class, FileTimeMixin.class)
                .enable(INDENT_OUTPUT)
                .disable(FAIL_ON_EMPTY_BEANS)
                .setSerializationInclusion(Include.NON_NULL)
                .setSerializationInclusion(Include.NON_EMPTY)
                .setSerializerFactory(serializerFactory);

        om.writeValue(out, this);

        return jsonld;
    }

    @Override
    public URI relativeToBundleRoot(URI uri) {
        return root.toUri().relativize(uri);
    }
}