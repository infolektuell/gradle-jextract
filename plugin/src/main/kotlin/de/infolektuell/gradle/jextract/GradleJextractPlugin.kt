package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.DefaultJextractResolver
import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.extensions.JextractResolver
import de.infolektuell.gradle.jextract.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent("${project.name}_${DownloadClient.SERVICE_NAME}", DownloadClient::class.java)
        val extension = project.extensions.create(JextractExtension.EXTENSION_NAME, JextractExtension::class.java)
        extension.generator.javaLanguageVersion.convention(JavaLanguageVersion.of(Jvm.current().javaVersionMajor ?: 22))
        project.extensions.findByType(JavaPluginExtension::class.java)?.let { extension.generator.javaLanguageVersion.convention(it.toolchain.languageVersion) }
        val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
        val resourceProvider: Provider<JextractResolver.Resource> = project.providers.of(DefaultJextractResolver::class.java) { spec ->
            spec.parameters.javaLanguageVersion.convention(extension.generator.javaLanguageVersion)
        }

        val userOutput = project.layout.projectDirectory.dir(project.gradle.gradleUserHomeDir.absolutePath)
        val downloadTask = project.tasks.register("downloadJextract", DownloadTask::class.java) { task ->
            task.description = "Downloads Jextract"
            task.downloadClient.convention(serviceProvider)
            task.usesService(serviceProvider)
            task.resource.convention(resourceProvider)
            task.target.convention(userOutput.dir("downloads").file(task.resource.map { it.fileName }))
        }

        val extractTask = project.tasks.register("extract", ExtractTask::class.java) { task ->
            task.source.convention(downloadTask.flatMap { it.target })
            task.target.convention(userOutput.dir("jextract").dir(extension.generator.javaLanguageVersion.map { "jextract-${it.asInt()}" }))
        }
        extension.generator.local.convention(extractTask.flatMap { it.target })

        extension.libraries.all { lib ->
            lib.useSystemLoadLibrary.convention(false)
            project.tasks.register("${lib.name}Jextract", GenerateBindingsTask::class.java) { task ->
                task.group = "Build"
                task.description = "Generates bindings for the ${lib.name} library using Jextract"
                task.outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/jextract/${lib.name}/main/java"))
                task.generator.location.convention(extension.generator.local)
                task.header.set(lib.header)
                task.definedMacros.set(lib.definedMacros)
                task.whitelist.set(lib.whitelist.mapProvider)
                task.argFile.set(lib.whitelist.argFile)
                task.targetPackage.set(lib.targetPackage)
                task.headerClassName.set(lib.headerClassName)
                task.includes.set(lib.includes)
                task.libraries.set(lib.libraries)
                task.useSystemLoadLibrary.set(lib.useSystemLoadLibrary)
                sourceSets?.named("main") { main ->
                    main.java.srcDir(task)
                    main.compileClasspath += project.files(task)
                    main.runtimeClasspath += project.files(task)
                }
            }
            project.tasks.register("${lib.name}DumpIncludes", DumpIncludesTask::class.java) { task ->
                task.group = "documentation"
                task.description = "Generates a dump of all symbols encountered in a header file"
                task.generator.location.convention(extension.generator.local)
                task.header.set(lib.header)
                task.argFile.convention(project.layout.buildDirectory.file("reports/jextract/${lib.name}-includes.txt"))
            }
        }
    }

    companion object {
        const val PLUGIN_NAME = "de.infolektuell.jextract"
    }
}
