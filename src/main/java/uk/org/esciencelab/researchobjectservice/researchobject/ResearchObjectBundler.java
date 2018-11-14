package uk.org.esciencelab.researchobjectservice.researchobject;

import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.writer.FetchWriter;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;

import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ResearchObjectBundler {
    private final ResearchObject researchObject;

    public ResearchObjectBundler(ResearchObject ro) {
        this.researchObject = ro;
    }

    public void bundle(OutputStream outputStream) throws Exception {
        Bundle bundle = Bundles.createBundle();
        for (Map.Entry<String, Object> entry : researchObject.getFields().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            URI ref = URI.create(value);
            Path out = bundle.getRoot().resolve(key);
            Bundles.setReference(out, ref);
        }

        Path bagPath = bag();
        Path roBagPath = bundle.getRoot().resolve("bag");

        Files.walk(bagPath)
                .forEach(source -> copy(source, roBagPath.resolve(bagPath.relativize(source).toString())));

        Path path = Files.createTempFile("bundle", ".zip");
        Bundles.closeAndSaveBundle(bundle, path);
        Files.copy(path, outputStream);
    }

    private Path bag() throws Exception {
        Path bagLocation = Files.createTempDirectory("baggo");
        StandardSupportedAlgorithms algorithm = StandardSupportedAlgorithms.MD5;
        Bag bag = BagCreator.bagInPlace(bagLocation, Arrays.asList(algorithm), false);

        ArrayList<FetchItem> fetchItems = new ArrayList<FetchItem>();
        for (Map.Entry<String, Object> entry : researchObject.getFields().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            URL ref = new URL(value);
            fetchItems.add(new FetchItem(ref, (long) -1, bagLocation.resolve(key)));
        }

        FetchWriter.writeFetchFile(fetchItems, bagLocation, bagLocation, StandardCharsets.UTF_8);

        return bagLocation;
    }

    private void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
