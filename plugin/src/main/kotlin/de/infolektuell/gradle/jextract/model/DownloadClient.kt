package de.infolektuell.gradle.jextract.model

import java.io.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers.ofInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration

/**
 * A service class for downloading files
 */
class DownloadClient {
    /** Indicates that the client couldn't download a given resource */
    class FailedDownloadException(message: String, val statusCode: Int = 404, err: Throwable? = null) : RuntimeException(message, err)
    /** Indicates that the integrity of a downloaded file couldn't be verified with the given checksum */
    class FailedIntegrityCheckException(message: String, err: Throwable? = null) : RuntimeException(message, err)

    /**
     * Data describing a downloadable file
     *
     * A resource consists of a [location][url]] and a [checksum] and [algorithm] to verify the integrity of the downloaded file.
     *
     * @constructor Creates a new resource
     */
    data class Resource(val url: URI, val checksum: String, val algorithm: String = "SHA-256") : Serializable {
        companion object {
            const val serialVersionUID: Long = 1
        }
    }

    /**
     * Downloads a [resource], checks its integrity, and stores it to a [target] path
     * @throws FailedDownloadException If the resource couldn't be downloaded
     * @throws FailedIntegrityCheckException If the expected and actual checksum of the downloaded file don't match.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun download(resource: Resource, target: Path) {
        val (url, checksum, algorithm) = resource
        val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(url)
            .GET()
            .build()
        val response = client.send(request, ofInputStream())
        if (response.statusCode() != 200) throw FailedDownloadException("Downloading from $url failed with status code ${response.statusCode()}.", 404)
        val md = MessageDigest.getInstance(algorithm)
        val input = DigestInputStream(response.body(), md)
        Files.createDirectories(target.parent)
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        val calculatedChecksum = input.messageDigest.digest().toHexString()
        val isValid = calculatedChecksum == checksum
        if (!isValid) {
            Files.deleteIfExists(target)
            throw FailedIntegrityCheckException("Data integrity of downloaded file $url could not be verified, checksums do not match.")
        }
    }
}
