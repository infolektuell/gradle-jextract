package de.infolektuell.gradle.jextract.tasks

import de.infolektuell.gradle.jextract.services.JextractDownloadClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import javax.inject.Inject

abstract class DownloadTask @Inject constructor(objects: ObjectFactory, private val fileSystem: FileSystemOperations, private val archive: ArchiveOperations) : DefaultTask() {
    @get:ServiceReference("download")
    abstract val jextractDownloadClient: Property<JextractDownloadClient>
    @get:Input
    abstract val url: Property<URI>
    @get:Input
    val checksum: Property<String> = objects.property(String::class.java)
    @get:Input
    abstract val algorithm: Property<String>

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objects.directoryProperty()
    @get:OutputDirectory
    val distributionDirectory: Provider<Directory> = outputDirectory.dir(checksum.map { it.substring(0, 8) })

    @TaskAction
    fun execute() {
        fileSystem.delete { spec ->
            spec.delete(distributionDirectory)
        }
        val archiveFile = jextractDownloadClient.get().download(url.get(), checksum.get(), algorithm.get())
        val tree = archive.tarTree(archiveFile)
        fileSystem.copy { spec ->
            spec.from(tree)
            spec.into(distributionDirectory)
        }
    }
}
