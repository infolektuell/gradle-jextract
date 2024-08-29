package de.infolektuell.jextract

import de.infolektuell.jextract.extensions.JextractExtension
import de.infolektuell.jextract.tasks.DownloadTask
import de.infolektuell.jextract.tasks.GenerateBindingsTask
import de.infolektuell.jextract.services.JextractDownloadClient
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("jextract", JextractExtension::class.java)
        extension.generator.distribution.apply {
            register("linux_aarch64")
            register("linux_x64")
            register("mac_aarch64")
            register("mac_x64")
            register("windows_aarch64")
            register("windows_x64")
        }
        val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
            if (toolchain.languageVersion.isPresent) {
                extension.generator.javaVersion(toolchain.languageVersion.get())
            } else {
                extension.generator.javaVersion(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
            }
        }
        extension.libraries.configureEach { lib ->
            lib.useSystemLoadLibrary.convention(false)
        }

        val userOutput = project.layout.projectDirectory.dir(project.gradle.gradleUserHomeDir.absolutePath)
        project.gradle.sharedServices.registerIfAbsent("download", JextractDownloadClient::class.java) { service ->
            service.parameters.outputDirectory.convention(userOutput.dir("downloads"))
        }
        project.tasks.withType(DownloadTask::class.java).configureEach { task ->
            task.group = "Download"
            task.outputDirectory.convention(userOutput.dir("jextract"))
        }
        project.tasks.withType(GenerateBindingsTask::class.java).configureEach { task ->
            task.group = "code generation"
            task.outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/jextract/main/java"))
        }
        extension.generator.distribution.all { resource ->
            project.tasks.register("${resource.name}Download", DownloadTask::class.java) { task ->
                task.description = "Downloads Jextract for ${resource.name} platform"
                task.url.set(resource.url)
                task.checksum.set(resource.integrity.checksum)
                task.algorithm.set(resource.integrity.algorithm)
            }
        }

        extension.libraries.all { lib ->
            project.tasks.register("${lib.name}Jextract", GenerateBindingsTask::class.java) { task ->
                task.description = "Generates bindings for the ${lib.name} library using Jextract"
                if (extension.generator.local.isPresent) {
                    task.generator.set(extension.generator.local)
                } else {
                    val downloadTask: DownloadTask = findDownloadTask(project)
                    task.dependsOn(downloadTask)
                    task.generator.set(downloadTask.distributionDirectory)
                }
                task.run {
                    header.set(lib.header)
                    targetPackage.set(lib.targetPackage.orElse(extension.targetPackage))
                    headerClassName.set(lib.headerClassName)
                    useSystemLoadLibrary.set(lib.useSystemLoadLibrary.orElse(extension.useSystemLoadLibrary))
                }
                javaExtension?.run {
                    sourceSets.getByName("main").java.srcDir(task)
                }
            }
        }
    }
    private fun findDownloadTask(project: Project): DownloadTask {
        val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
        val currentArch = DefaultNativePlatform.getCurrentArchitecture()
        val downloadTasks = project.tasks.withType(DownloadTask::class.java)
        if (currentOs.isLinux) {
            if (currentArch.isArm64) {
                return downloadTasks.findByName("linux_aarch64Download") ?: downloadTasks.getByName("linux_x64Download")
            }
            return downloadTasks.getByName("linux_x64Download")
        }
        else if (currentOs.isMacOsX) {
            if (currentArch.isArm64) {
                return downloadTasks.findByName("mac_aarch64Download") ?: downloadTasks.getByName("mac_x64Download")
            }
            return downloadTasks.getByName("mac_x64Download")
        }
        if (currentOs.isWindows) {
            if (currentArch.isArm64) {
                return downloadTasks.findByName("windows_aarch64Download") ?: downloadTasks.getByName("windows_x64Download")
            }
            return downloadTasks.getByName("windows_x64Download")
        }
        return downloadTasks.getByName("windows_x64Download")
    }
}
