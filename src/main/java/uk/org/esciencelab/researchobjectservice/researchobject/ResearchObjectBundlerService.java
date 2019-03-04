package uk.org.esciencelab.researchobjectservice.researchobject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.org.esciencelab.researchobjectservice.bagit.ResearchObjectBaggerService;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ResearchObjectBundlerService {
    @Autowired
    private ResearchObjectBaggerService researchObjectBaggerService;

    public void bundle(ResearchObject researchObject, OutputStream outputStream) throws Exception {
//        Bundle bundle = Bundles.createBundle();
//        for (Map.Entry<String, Object> entry : researchObject.getFields().entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue().toString();
//            URI ref = URI.create(value);
//            Path out = bundle.getRoot().resolve(key);
//            Bundles.setReference(out, ref);
//        }
//
//        Path bagPath = researchObjectBaggerService.bag(researchObject);
//        Path roBagPath = bundle.getRoot().resolve("bag");
//
//        Files.walk(bagPath)
//                .forEach(source -> copy(source, roBagPath.resolve(bagPath.relativize(source).toString())));
//
//        Path path = Files.createTempFile("bundle", ".zip");
//        Bundles.closeAndSaveBundle(bundle, path);
//        Files.copy(path, outputStream);
    }

    private void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
