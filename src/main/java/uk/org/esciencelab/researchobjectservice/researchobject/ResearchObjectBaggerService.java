package uk.org.esciencelab.researchobjectservice.researchobject;

import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.writer.FetchWriter;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ResearchObjectBaggerService {
    public Path bag(ResearchObject researchObject) throws Exception {
        Path bagLocation = Files.createTempDirectory("bag");
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
}
