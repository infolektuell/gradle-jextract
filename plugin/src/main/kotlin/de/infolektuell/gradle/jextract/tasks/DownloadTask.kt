package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.net.URI
import java.nio.file.Files

abstract class DownloadTask : DefaultTask() {
    @get:Internal
    abstract val downloadClient: Property<DownloadClient>
    @get:Input
    abstract val source: Property<URI>
    @get:OutputFile
    abstract val target: RegularFileProperty
    @get:Optional
    @get:Input
    abstract val integrity: MapProperty<String, String>

    @TaskAction
    fun run() {
        Files.createDirectories(target.asFile.get().parentFile.toPath())
        downloadClient.get().download(source.get(), target.get(), integrity.get())
    }
}
