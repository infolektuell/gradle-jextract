package de.infolektuell.jextract.services

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest

abstract class JextractDownloadClient : BuildService<JextractDownloadClient.Parameters> {
    interface Parameters : BuildServiceParameters {
        val outputDirectory: DirectoryProperty
    }
    private val client: HttpClient = HttpClient.newHttpClient()
    private val filesDirectory = parameters.outputDirectory.dir("files")

    fun download(url: URI, checksum: String, algorithm: String): RegularFile {
        val outFile = filesDirectory.get().file(url.path.replaceBeforeLast("/", "").trim('/'))
        Files.createDirectories(filesDirectory.get().asFile.toPath())
        val downloaded = verify(outFile, checksum, algorithm)
        if (downloaded) return outFile
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(url)
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) throw GradleException("Downloading from $url failed with status code ${response.statusCode()}.")
        val isValid = copyVerify(response.body(), outFile, checksum, algorithm)
        if (!isValid) throw GradleException("Data integrity of downloaded file $url could not be verified, checksums do not match.")
        return outFile
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun copyVerify(from: InputStream, file: RegularFile, checksum: String, algorithm: String): Boolean {
        val path = file.asFile.toPath()
        val md = MessageDigest.getInstance(algorithm)
        val input = DigestInputStream(from, md)
        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
        val calculatedChecksum = input.messageDigest.digest().toHexString()
        if (calculatedChecksum != checksum) Files.deleteIfExists(path)
        return calculatedChecksum == checksum
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun verify(file: RegularFile, checksum: String, algorithm: String): Boolean {
        val path = file.asFile.toPath()
        if (Files.notExists(path)) return false
        val md = MessageDigest.getInstance(algorithm)
        DigestInputStream(Files.newInputStream(path), md).use { input ->
            input.readAllBytes()
        }
        val calculatedChecksum = md.digest().toHexString()
        if (calculatedChecksum != checksum) Files.deleteIfExists(path)
        return calculatedChecksum == checksum
    }
}
