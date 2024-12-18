package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.InputStream
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
import javax.inject.Inject

@CacheableTask
abstract class DownloadTask : DefaultTask() {
    abstract class Resource @Inject constructor(objects: ObjectFactory) {
        @get:Input
        val url: Property<URI> = objects.property(URI::class.java)
        @get:Internal
        val fileName: Provider<String> = url.map { it.path.replaceBeforeLast('/', "").trim('/') }
        @get:Input
        abstract val checksum: Property<String>
        @get:Input
        abstract val algorithm: Property<String>
    }
    @get:Nested
    abstract val resource: Resource
    @get:OutputFile
    abstract val target: RegularFileProperty

    @TaskAction
    protected fun download() {
        val url = resource.url.get()
        val checksum = resource.checksum.get()
        val algorithm = resource.algorithm.get()
        val targetPath = target.get().asFile.toPath()
        Files.createDirectories(targetPath.parent)
        download(url, checksum, algorithm, targetPath)
    }

    private fun download(source: URI, checksum: String, algorithm: String, target: Path) {
        val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(source)
            .GET()
            .build()
        val response = client.send(request, ofInputStream())
        if (response.statusCode() != 200) throw GradleException("Downloading from $source failed with status code ${response.statusCode()}.")
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
}
