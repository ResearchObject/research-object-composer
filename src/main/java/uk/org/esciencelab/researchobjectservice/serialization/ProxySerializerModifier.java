package uk.org.esciencelab.researchobjectservice.serialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.List;

/**
 * A class to modify how the ROBundle Proxy bean is serialized.
 * This is needed purely to fix a bug where the "folder" path does not have a trailing slash.
 */
public class ProxySerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);
            if (writer.getName().equals("folder")) {
                beanProperties.set(i, new TrailingSlashWriter(writer));
            }
        }
        return beanProperties;
    }
}
