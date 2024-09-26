package de.infolektuell.gradle.jextract.tasks

import de.infolektuell.gradle.jextract.extensions.JextractResolver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration

abstract class DownloadTask : DefaultTask() {
    @get:Input
    abstract val resource: Property<JextractResolver.Resource>
    @get:OutputFile
    abstract val target: RegularFileProperty
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build()

    @TaskAction
    fun download() {
        val url = resource.get().url
        val checksum = resource.get().checksum
        val algorithm = resource.get().algorithm
        val targetPath = target.get().asFile.toPath()
        Files.createDirectories(targetPath.parent)
        download(url, checksum, algorithm, targetPath)
    }

    private fun download(source: URI, checksum: String, algorithm: String, target: Path) {
        val downloaded = verify(target, checksum, algorithm)
        if (downloaded) return
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(source)
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) throw GradleException("Downloading from $source failed with status code ${response.statusCode()}.")
        println(response.version())
        val isValid = copyVerify(response.body(), target, checksum, algorithm)
        if (!isValid) throw GradleException("Data integrity of downloaded file $source could not be verified, checksums do not match.")
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun copyVerify(from: InputStream, file: Path, checksum: String, algorithm: String): Boolean {
        val md = MessageDigest.getInstance(algorithm)
        val input = DigestInputStream(from, md)
        Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING)
        val calculatedChecksum = input.messageDigest.digest().toHexString()
        if (calculatedChecksum != checksum) Files.deleteIfExists(file)
        return calculatedChecksum == checksum
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun verify(file: Path, checksum: String, algorithm: String): Boolean {
        if (Files.notExists(file)) return false
        val md = MessageDigest.getInstance(algorithm)
        DigestInputStream(Files.newInputStream(file), md).use { input ->
            input.readAllBytes()
        }
        val calculatedChecksum = md.digest().toHexString()
        val isValid = calculatedChecksum == checksum
        if (!isValid) Files.deleteIfExists(file)
        return isValid
    }
}
