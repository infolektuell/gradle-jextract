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
import java.util.HexFormat;
import java.util.Objects;

/// A service class for downloading files
public class DownloadClient {

    /// Describes a downloadable file
    /// @param url The remote location of the downloadable file
    /// @param checksum A checksum to verify the integrity of the downloaded file
    /// @param algorithm The algorithm that was used to generate the checksum, e.g., SHA-256
    public record Resource(URI url, String checksum, String algorithm) implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L;
    }

    ///  Creates a new instance
    public DownloadClient() { super(); }

    /// Downloads a resource, checks its integrity, and stores it to a target path
    /// @param resource Data describing the download location and file checksum
    /// @param target The file path to store the downloaded file
    public void download(Resource resource, Path target) {
        var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1);
        try (HttpClient client = builder.build()) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(resource.url)
                .GET()
                .build();
            var response = client.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() != 200)
                throw new RuntimeException(String.format("Downloading from %s failed with status code %d.", resource.url, response.statusCode()));
            var md = MessageDigest.getInstance(resource.algorithm);
            var input = new DigestInputStream(response.body(), md);
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            String calculatedChecksum = HexFormat.of().formatHex(input.getMessageDigest().digest());
            if (!Objects.equals(resource.checksum, calculatedChecksum)) {
                Files.deleteIfExists(target);
                throw new RuntimeException(String.format("Data integrity of downloaded file %s could not be verified, checksums do not match.", resource.url));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /// Checks that a given file is the downloaded file for a given resource by comparing their checksums.
    /// @param resource Data describing the remote downloadable file
    /// @param file The file to verify
    /// @return True if the checksums match, false otherwise.
    public boolean verify(Resource resource, Path file) {
        if (!Files.exists(file)) return false;
        try (var s = Files.newInputStream(file)) {
            var md = MessageDigest.getInstance(resource.algorithm);
            var input = new DigestInputStream(s, md);
            input.readAllBytes();
            String calculatedChecksum = HexFormat.of().formatHex(input.getMessageDigest().digest());
            return Objects.equals(resource.checksum, calculatedChecksum);
        } catch (Exception ignored) {
            return false;
        }
    }
}
