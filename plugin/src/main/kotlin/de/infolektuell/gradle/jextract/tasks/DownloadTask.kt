package de.infolektuell.gradle.jextract.tasks

import de.infolektuell.gradle.jextract.service.DownloadClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Downloads a [file][resource] to a [path][target] using [DownloadClient] */
@CacheableTask
abstract class DownloadTask : DefaultTask() {
    @get:Input
    abstract val resource: Property<DownloadClient.Resource>
    @get:OutputFile
    abstract val target: RegularFileProperty

    @TaskAction
    protected fun download() {
        DownloadClient().download(resource.get(), target.get().asFile.toPath())
    }
}
