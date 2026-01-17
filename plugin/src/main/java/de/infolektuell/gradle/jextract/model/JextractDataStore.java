package de.infolektuell.gradle.jextract.model;

import de.infolektuell.gradle.jextract.model.DownloadClient.Resource;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class JextractDataStore {
    /**
     * Tries to create an instance of JextractDataStore using data from a properties file
     * @param path The file that contains the Jextract distribution data to be loaded
     * @return a JextractDataStore instance
     */
    public static JextractDataStore create(Path path) {
        try (var f = Files.newBufferedReader(path)) {
            var data = new Properties();
            data.load(f);
            return new JextractDataStore(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tries to create an instance of JextractDataStore using the default distribution data
     * @return a JextractDataStore instance
     */
    public static JextractDataStore create() {
        try (InputStream s = JextractDataStore.class.getResourceAsStream("/jextract.properties")) {
            var data = new Properties();
            data.load(s);
            return new JextractDataStore(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Platform platform = Platform.getCurrentPlatform();
    private final Properties data;
    JextractDataStore(Properties data) {
        this.data = data;
    }

    /**
     * Returns the file name of the Jextract executable for the current operating system
     */
    public String getExecutableFilename() {
        return platform.isWindows() ? "jextract.bat" : "jextract";
    }

    /**
     * Returns the matching Jextract version for a given [Java major version][javaVersion]
     */
    public int version(int javaVersion) {
        if (javaVersion == 25) {
            return 25;
        }
        return Integer.max(Integer.min(javaVersion, 22), 19);
    }

    /**
     * Returns a downloadable resource for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data
     */
    public Resource resource(int javaVersion) {
        int version = version(javaVersion);
        String os = platform.operatingSystem().name().toLowerCase();
        String arch = platform.architecture().name().toLowerCase();
        String base = String.format("jextract.%d.%s.%s", version, os, arch);
        String baseFallback = String.format("jextract.%d.%s.%s", version, os, "x64");
        String url = data.getProperty(base + ".url", data.getProperty(baseFallback + ".url"));
        String checksum = data.getProperty(base + ".sha-256", data.getProperty(baseFallback + ".sha-256"));
        return new Resource(URI.create(url), checksum, "SHA-256");
    }

    /**
     * Returns an archive filename for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data
     */
    public String filename(int javaVersion) {
        int version = version(javaVersion);
        String os = platform.operatingSystem().name().toLowerCase();
        String arch = platform.architecture().name().toLowerCase();
        String base = String.format("jextract.%d.%s.%s", version, os, arch);
        String baseFallback = String.format("jextract.%d.%s.%s", version, os, "x64");
        String url = data.getProperty(base + ".url", data.getProperty(baseFallback + ".url"));
        var segments = url.split("/");
        return segments[segments.length - 1];
    }
}
