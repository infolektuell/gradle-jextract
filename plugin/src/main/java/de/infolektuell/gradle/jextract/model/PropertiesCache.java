package de.infolektuell.gradle.jextract.model;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class PropertiesCache {
    private Properties defaultProperties;
    private final Map<Path, Properties> data = new HashMap<>();
    private final String defaultResource;

    PropertiesCache(String defaultResource) {
        super();
        this.defaultResource = defaultResource;
    }

    private Properties getDefaultProperties() {
        if (defaultProperties != null) {
            return defaultProperties;
        }
        try (InputStream s = PropertiesCache.class.getResourceAsStream(defaultResource)) {
            var properties = new Properties();
            properties.load(s);
            defaultProperties = properties;
            return defaultProperties;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load default distributions from resource");
        }
    }

    Properties getProperties(Path path) {
        if (path == null) {
            return getDefaultProperties();
        }
        return data.computeIfAbsent(path, k -> {
            try (var f = Files.newBufferedReader(k)) {
                var p = new Properties(getDefaultProperties());
                p.load(f);
                return p;
            } catch(Exception e) {
                throw new RuntimeException("Couldn't load properties from path");
            }
        });
    }
}
