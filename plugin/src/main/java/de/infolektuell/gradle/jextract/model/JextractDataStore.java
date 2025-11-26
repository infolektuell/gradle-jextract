package de.infolektuell.gradle.jextract.model;

import de.infolektuell.gradle.jextract.model.DownloadClient.Resource;
import java.nio.file.Path;
import java.net.URI;

public class JextractDataStore {
    private final Platform platform = Platform.getCurrentPlatform();
    private final PropertiesCache distributionCache = new PropertiesCache("/jextract.properties");

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

    /** Returns a downloadable resource for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data */
    public Resource resource(int javaVersion, Path file) {
        var data = distributionCache.getProperties(file);
        int version = version(javaVersion);
        String os = platform.operatingSystem().name().toLowerCase();
        String arch = platform.architecture().name().toLowerCase();
        String base = String.join(".", "jextract", Integer.toString(version), os);
        String url  = data.getProperty(String.join(".", base, arch, "url"), data.getProperty(String.join(".", base, "x64", "url")));
        String checksum  = data.getProperty(String.join(".", base, arch, "checksum"), data.getProperty(String.join(".", base, "x64", "checksum")));
        return new Resource(URI.create(url), checksum, "SHA-256");
    }

    /** Returns an archive filename for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data */
    public String filename(int javaVersion, Path file) {
        var data = distributionCache.getProperties(file);
        int version = version(javaVersion);
        String os = platform.operatingSystem().name().toLowerCase();
        String arch = platform.architecture().name().toLowerCase();
        String base = String.join(".", "jextract", Integer.toString(version), os);
        String url  = data.getProperty(String.join(".", base, arch, "url"), data.getProperty(String.join(".", base, "x64", "url")));
        var segments = url.split("/");
        return segments[segments.length - 1];
    }
}
