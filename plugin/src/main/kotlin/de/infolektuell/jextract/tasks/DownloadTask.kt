package de.infolektuell.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.net.URI
import java.nio.file.Files
import java.security.DigestOutputStream
import java.security.MessageDigest
import javax.inject.Inject

abstract class DownloadTask @Inject constructor(private val fileSystem: FileSystemOperations, private val archive: ArchiveOperations) : DefaultTask() {
    @get:Input
    abstract val src: Property<URI>
    @get:Input
    abstract val checksum: Property<String>
    @get:Input
    abstract val algorithm: Property<String>
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:Optional
    @get:OutputFile
    abstract val script: RegularFileProperty
    @OptIn(ExperimentalStdlibApi::class)
    @TaskAction
    fun execute() {
        fileSystem.delete { spec ->
            spec.delete(outputDirectory)
        }
        Files.createDirectories(outputDirectory.get().asFile.toPath())
        val archiveFile  = outputDirectory.get().file(ARCHIVE_NAME)
        src.get().toURL().openStream().use { input ->
            val md = MessageDigest.getInstance(algorithm.get())
            val output = DigestOutputStream(Files.newOutputStream(archiveFile.asFile.toPath()), md)
            input.copyTo(output)
            val calculatedChecksum = output.messageDigest.digest().toHexString()
            if (calculatedChecksum != checksum.get()) {
                throw GradleException("Checksum verification failed")
            }
        }
        val tree = archive.tarTree(archiveFile)
        fileSystem.copy { spec ->
            spec.from(tree)
            spec.into(outputDirectory)
        }
    }
    companion object {
        const val ARCHIVE_NAME = "jextract.tgz"
    }
}
