package uk.org.esciencelab.researchobjectservice.bagit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.writer.BagWriter;
import org.springframework.stereotype.Service;
import uk.org.esciencelab.researchobjectservice.researchobject.ResearchObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ResearchObjectBaggerService {

    //private static String BAGGABLE_TYPE = "classpath://public/schemas/base_metadata_schema.json#/definitions/RemoteItem";

    public Path bag(ResearchObject researchObject) throws Exception {
        Path bagLocation = Files.createTempDirectory("bag");

        Map<SupportedAlgorithm, Map<Path, String>> checksumMap = new HashMap<>();
        checksumMap.put(StandardSupportedAlgorithms.MD5, new HashMap<>());
        checksumMap.put(StandardSupportedAlgorithms.SHA256, new HashMap<>());
        checksumMap.put(StandardSupportedAlgorithms.SHA512, new HashMap<>());

        Bag bag = BagCreator.bagInPlace(bagLocation, Arrays.asList(StandardSupportedAlgorithms.MD5,
                StandardSupportedAlgorithms.SHA256, StandardSupportedAlgorithms.SHA512), false);

        ArrayList<BagEntry> entries = gatherBagEntries(bagLocation, researchObject);
        ArrayList<FetchItem> fetchItems = new ArrayList<>(entries.size());

        for (BagEntry entry : entries) {
            for (SupportedAlgorithm alg : checksumMap.keySet()) {
                String checksum = entry.getChecksum(alg);
                if (checksum != null) {
                    checksumMap.get(alg).put(entry.getFilepath(), checksum);
                }
            }

            fetchItems.add(entry.getFetchItem());
        }

        Set<Manifest> manifests = new HashSet<>();
        for (SupportedAlgorithm alg : checksumMap.keySet()) {
            if (!checksumMap.get(alg).isEmpty()) {
                Manifest manifest = new Manifest(alg);
                manifest.setFileToChecksumMap(checksumMap.get(alg));
                manifests.add(manifest);
            }
        }

        bag.setItemsToFetch(fetchItems);
        bag.setPayLoadManifests(manifests);

        BagWriter.write(bag, bagLocation);

        return bagLocation;
    }

    public void bagToZip(ResearchObject researchObject, OutputStream outputStream) throws Exception {
        Path bagLocation = bag(researchObject);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        // TODO: Got to be a better way of doing this path manipulation
        Files.walk(bagLocation)
                .forEach(source -> zipTo(source, researchObject.getId() + "/" + bagLocation.relativize(source).toString(), zipOutputStream));
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    private void zipTo(Path source, String entryName, ZipOutputStream zipOutputStream) {
        try {
            if (source.toFile().isDirectory()) {
                return;
            }
            InputStream inputStream = Files.newInputStream(source);

            ZipEntry zipEntry = new ZipEntry(entryName);

            zipOutputStream.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;

            while ((length = inputStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
            zipOutputStream.closeEntry();
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private ArrayList<BagEntry> gatherBagEntries(Path basePath, ResearchObject researchObject) {
        ArrayNode a = (ArrayNode) researchObject.getContent().get("data"); // TODO: Un-hard-code
        ArrayList<BagEntry> x = new ArrayList<>(a.size());

        for (JsonNode jsonNode : a) {
            try {
                BagEntry b = new BagEntry(basePath.resolve("data"), jsonNode); // TODO: Un-hard-code
                for (JsonNode checksumNode : jsonNode.get("checksums")) {
                    if (checksumNode.get("type").asText().equals("md5")) {
                        b.setChecksum(StandardSupportedAlgorithms.MD5, checksumNode.get("checksum").asText());
                    } else if (checksumNode.get("type").asText().equals("sha256")) {
                        b.setChecksum(StandardSupportedAlgorithms.SHA256, checksumNode.get("checksum").asText());
                    } else if (checksumNode.get("type").asText().equals("sha512")) {
                        b.setChecksum(StandardSupportedAlgorithms.SHA512, checksumNode.get("checksum").asText());
                    }
                }
                x.add(b);
            } catch (MalformedURLException e) {
            }
        }

        return x;
    }
}
