package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import org.apache.taverna.robundle.manifest.Proxy;

import java.nio.file.Path;

/**
 * A class to add a trailing slash to an ROBundle Proxy's folder path.
 */
public class TrailingSlashWriter extends BeanPropertyWriter {
    BeanPropertyWriter _writer;

    public TrailingSlashWriter(BeanPropertyWriter w) {
        super(w);
        _writer = w;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator generator, SerializerProvider provider) throws Exception {
        Path folder = ((Proxy) bean).getFolder();
        if (folder != null) {
            String value = folder.toString();
            if (!folder.endsWith("/")) {
                value = value + "/";
            }

            generator.writeStringField("folder", value);
        }
    }
}