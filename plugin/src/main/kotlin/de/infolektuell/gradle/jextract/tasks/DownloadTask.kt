package de.infolektuell.gradle.jextract.tasks

import de.infolektuell.gradle.jextract.extensions.JextractResolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.nio.file.Files

abstract class DownloadTask : DefaultTask() {
    @get:Internal
    abstract val downloadClient: Property<DownloadClient>
    @get:Input
    abstract val resource: Property<JextractResolver.Resource>
    @get:OutputFile
    abstract val target: RegularFileProperty

    @TaskAction
    fun download() {
        Files.createDirectories(target.asFile.get().parentFile.toPath())
        val url = resource.get().url
        val checksum = resource.get().checksum
        val algorithm = resource.get().algorithm
        downloadClient.get().download(url, checksum, algorithm, target.get())
    }
}
