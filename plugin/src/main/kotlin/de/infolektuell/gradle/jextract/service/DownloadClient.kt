package de.infolektuell.gradle.jextract.service

import org.gradle.api.GradleException
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

class DownloadClient {
    data class Resource(val url: URI, val checksum: String, val algorithm: String = "SHA-256") : Serializable {
        val filename: String get() = url.path.replaceBeforeLast('/', "").trim('/')
        companion object {
            const val serialVersionUID: Long = 0
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun download(resource: Resource, target: Path) {
        val (url, checksum, algorithm) = resource
        val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(url)
            .GET()
            .build()
        val response = client.send(request, ofInputStream())
        if (response.statusCode() != 200) throw GradleException("Downloading from $url failed with status code ${response.statusCode()}.")
        val md = MessageDigest.getInstance(algorithm)
        val input = DigestInputStream(response.body(), md)
        Files.createDirectories(target.parent)
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        val calculatedChecksum = input.messageDigest.digest().toHexString()
        val isValid = calculatedChecksum == checksum
        if (!isValid) {
            Files.deleteIfExists(target)
            throw GradleException("Data integrity of downloaded file $url could not be verified, checksums do not match.")
        }
    }
}
