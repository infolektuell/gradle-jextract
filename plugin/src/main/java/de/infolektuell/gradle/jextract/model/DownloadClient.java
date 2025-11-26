package de.infolektuell.gradle.jextract.model;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Objects;

/**
 * A service class for downloading files
 */
public class DownloadClient {

    /**
     * Data describing a downloadable file
     * A resource consists of a [location][url] and a [checksum] and [algorithm] to verify the integrity of the downloaded file.
     */
    public record Resource(URI url, String checksum, String algorithm) implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L;
    }

    /**
     * Downloads a [resource], checks its integrity, and stores it to a [target] path
     */
    public void download(Resource resource, Path target) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(resource.url)
            .GET()
            .build();
        try {
            var response = client.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) throw new RuntimeException("Downloading from $url failed with status code ${response.statusCode()}.");
            var md = MessageDigest.getInstance(resource.algorithm);
            var input = new DigestInputStream(response.body(), md);
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            byte[] digest = input.getMessageDigest().digest();
            StringBuilder calculatedChecksum = new StringBuilder();
            for (byte b : digest) {
                calculatedChecksum.append(String.format("%02x", b));
            }
            boolean isValid = Objects.equals(resource.checksum, calculatedChecksum.toString());
            if (!isValid) {
                Files.deleteIfExists(target);
                throw new RuntimeException("Data integrity of downloaded file $url could not be verified, checksums do not match.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
