package de.infolektuell.gradle.download

import de.infolektuell.gradle.download.extensions.DownloadExtension
import de.infolektuell.gradle.download.tasks.DownloadClient
import de.infolektuell.gradle.download.tasks.DownloadTask
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class GradleDownloadPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent("${project.name}_${DownloadClient.SERVICE_NAME}", DownloadClient::class.java)
        project.tasks.withType(DownloadTask::class.java).configureEach { task ->
            task.group = "download"
            task.downloadClient.set(serviceProvider)
            task.usesService(serviceProvider)
        }
        val extension = project.extensions.create(DownloadExtension.EXTENSION_NAME, DownloadExtension::class.java)
        extension.targetDirectory.convention(project.layout.buildDirectory.dir("downloads"))
        extension.resources.all { resource ->
            resource.target.convention(extension.targetDirectory.file(resource.source.map { it.path.replaceBeforeLast("/", "").trim('/') }))
            project.tasks.register("${resource.name}Download", DownloadTask::class.java) { task ->
                task.source.set(resource.source)
                task.target.set(resource.target)
                task.integrity.set(resource.integrity)
            }
        }
    }
    companion object {
        const val PLUGIN_NAME = "de.infolektuell.download"
    }
}
