package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension
import de.infolektuell.gradle.jextract.service.JextractDataStore
import de.infolektuell.gradle.jextract.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val dataStore = JextractDataStore()
        val extension = project.extensions.create(JextractExtension.EXTENSION_NAME, JextractExtension::class.java)
        extension.generator.javaLanguageVersion.convention(JavaLanguageVersion.of(Jvm.current().javaVersionMajor ?: 22))
        val versionProvider = extension.generator.javaLanguageVersion.map { dataStore.version(it.asInt()) }
        val resourceProvider = versionProvider.map { dataStore.resource(it) }
        extension.output.convention(project.layout.buildDirectory.dir("generated/sources/jextract"))
        extension.generateSourceFiles.convention(false)

        val downloadTask = project.tasks.register("downloadJextract", DownloadTask::class.java) { task ->
            task.description = "Downloads Jextract"
            task.resource.convention(resourceProvider)
            task.target.convention(task.resource.flatMap { project.layout.buildDirectory.file("downloads/${it.filename}") })
        }

        val extractTask = project.tasks.register("extract", ExtractTask::class.java) { task ->
            task.source.convention(downloadTask.flatMap { it.target })
            task.target.convention(project.layout.buildDirectory.dir("jextract"))
        }
        extension.generator.local.convention(extractTask.flatMap { it.target })

        extension.libraries.configureEach { lib ->
            lib.useSystemLoadLibrary.convention(false)
            lib.output.convention(extension.output.dir(lib.name))
            lib.generateSourceFiles.convention(extension.generateSourceFiles)
        }

        project.tasks.withType(JextractBaseTask::class.java) { task ->
            task.distribution.convention(extension.generator.local)
        }

        val jextractGenerateTasks = mutableMapOf<String, TaskProvider<JextractGenerateTask>>()
        extension.libraries.all { lib ->
            val generateTaskProvider = project.tasks.register("${lib.name}JextractGenerateBindings", JextractGenerateTask::class.java) { task ->
                task.header.set(lib.header)
                task.includes.set(lib.includes)
                task.definedMacros.set(lib.definedMacros)
                task.headerClassName.set(lib.headerClassName)
                task.targetPackage.set(lib.targetPackage)
                task.whitelist.run {
                    put("function", lib.whitelist.functions)
                    put("constant", lib.whitelist.constants)
                    put("struct", lib.whitelist.structs)
                    put("union", lib.whitelist.unions)
                    put("typedef", lib.whitelist.typedefs)
                    put("var", lib.whitelist.variables)
                }
                task.argFile.set(lib.whitelist.argFile)
                task.libraries.set(lib.libraries)
                task.useSystemLoadLibrary.set(lib.useSystemLoadLibrary)
                task.generateSourceFiles.set(lib.generateSourceFiles)
                task.sources.set(lib.output)
            }
            jextractGenerateTasks[lib.name] = generateTaskProvider
            project.tasks.register("${lib.name}JextractDumpIncludes", JextractDumpIncludesTask::class.java) { task ->
                task.header.set(lib.header)
                task.includes.set(lib.includes)
                task.argFile.set(project.layout.buildDirectory.file("reports/jextract/${lib.name}-includes.txt"))
            }
        }

        project.pluginManager.withPlugin("java") {
            project.extensions.findByType(JavaPluginExtension::class.java)?.let { extension.generator.javaLanguageVersion.convention(it.toolchain.languageVersion) }
            project.extensions.findByType(SourceSetContainer::class.java)?.all { s ->
                val sourceSetExtension = s.extensions.create(SourceSetExtension.EXTENSION_NAME, SourceSetExtension::class.java, project.objects)
                sourceSetExtension.libraries.all { lib ->
                    jextractGenerateTasks[lib.name]?.flatMap { it.sources }?.also {
                        s.java.srcDir(it)
                        s.resources.srcDir(it)
                        s.compileClasspath += project.files(it)
                        s.runtimeClasspath += project.files(it)
                    }
                }
            }
        }

        project.tasks.register("jextract") { task ->
            task.group = "Build"
            task.description = "Generates bindings for all configured libraries"
            task.dependsOn(project.tasks.withType(JextractGenerateTask::class.java))
        }

        project.tasks.register("dumpIncludes") { task ->
            task.group = "documentation"
            task.description = "Generates a dump of all symbols encountered in a header file"
            task.dependsOn(project.tasks.withType(JextractDumpIncludesTask::class.java))
        }
    }

    companion object {
        const val PLUGIN_NAME = "de.infolektuell.jextract"
    }
}
