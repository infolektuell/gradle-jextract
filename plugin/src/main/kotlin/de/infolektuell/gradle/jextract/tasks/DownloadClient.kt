package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
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

abstract class DownloadClient : BuildService<BuildServiceParameters.None> {
    private val client: HttpClient = HttpClient.newHttpClient()

    fun download(source: URI, target: RegularFile, integrity: Map<String, String>) {
        val targetPath = target.asFile.toPath()
        val downloaded = integrity.entries.firstOrNull()?.let { verify(targetPath, it) } ?: false
        if (downloaded) return
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(source)
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) throw GradleException("Downloading from $source failed with status code ${response.statusCode()}.")
        val isValid = integrity.entries.firstOrNull()?.let { copyVerify(response.body(), targetPath, it) }
        if (isValid == false) throw GradleException("Data integrity of downloaded file $source could not be verified, checksums do not match.")
        if (isValid == null) {
            Files.copy(response.body(), targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun copyVerify(from: InputStream, file: Path, integrity: Map.Entry<String, String>): Boolean {
        val (algorithm, checksum) = integrity
        val md = MessageDigest.getInstance(algorithm)
        val input = DigestInputStream(from, md)
        Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING)
        val calculatedChecksum = input.messageDigest.digest().toHexString()
        if (calculatedChecksum != checksum) Files.deleteIfExists(file)
        return calculatedChecksum == checksum
    }
    @OptIn(ExperimentalStdlibApi::class)
    private fun verify(file: Path, integrity: Map.Entry<String, String>): Boolean {
        val (algorithm, checksum) = integrity
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
    companion object {
        const val SERVICE_NAME = "downloadClient"
    }
}
