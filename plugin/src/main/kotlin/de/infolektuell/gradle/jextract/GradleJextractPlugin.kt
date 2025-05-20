package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension
import de.infolektuell.gradle.jextract.service.JextractDataStore
import de.infolektuell.gradle.jextract.tasks.DownloadTask
import de.infolektuell.gradle.jextract.tasks.DumpIncludesTask
import de.infolektuell.gradle.jextract.tasks.ExtractTask
import de.infolektuell.gradle.jextract.tasks.GenerateBindingsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
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

        val jextractTask = project.tasks.register("jextract", GenerateBindingsTask::class.java) { task ->
            task.group = "Build"
            task.description = "Generates bindings for all configured libraries"
            task.generator.version.convention(versionProvider)
            task.generator.location.convention(extension.generator.local)
        }

        val dumpIncludesTask = project.tasks.register("dumpIncludes", DumpIncludesTask::class.java) { task ->
            task.group = "documentation"
            task.description = "Generates a dump of all symbols encountered in a header file"
            task.generator.version.convention(versionProvider)
            task.generator.location.convention(extension.generator.local)
        }

        extension.libraries.all { lib ->
            lib.useSystemLoadLibrary.convention(false)
            lib.output.convention(extension.output.dir("${lib.name}"))
            lib.generateSourceFiles.convention(extension.generateSourceFiles)
            jextractTask.configure { task ->
                val config = project.objects.newInstance(GenerateBindingsTask.LibraryConfig::class.java).apply {
                    header.set(lib.header)
                    includes.set(lib.includes)
                    definedMacros.set(lib.definedMacros)
                    headerClassName.set(lib.headerClassName)
                    targetPackage.set(lib.targetPackage)
                    whitelist.put("function", lib.whitelist.functions)
                    whitelist.put("constant", lib.whitelist.constants)
                    whitelist.put("struct", lib.whitelist.structs)
                    whitelist.put("union", lib.whitelist.unions)
                    whitelist.put("typedef", lib.whitelist.typedefs)
                    whitelist.put("var", lib.whitelist.variables)
                    argFile.set(lib.whitelist.argFile)
                    libraries.set(lib.libraries)
                    useSystemLoadLibrary.set(lib.useSystemLoadLibrary)
                    generateSourceFiles.set(lib.generateSourceFiles)
                    sources.set(lib.output)
                }
                task.libraries.add(config)
            }
            dumpIncludesTask.configure { task ->
                val config = project.objects.newInstance(DumpIncludesTask.LibraryConfig::class.java).apply {
                    header.set(lib.header)
                    includes.set(lib.includes)
                    argFile.set(project.layout.buildDirectory.file("reports/jextract/${lib.name}-includes.txt"))
                }
                task.libraries.add(config)
            }
        }

        project.pluginManager.withPlugin("java") {
            project.extensions.findByType(JavaPluginExtension::class.java)?.let { extension.generator.javaLanguageVersion.convention(it.toolchain.languageVersion) }
            project.extensions.findByType(SourceSetContainer::class.java)?.all { s ->
                val sourceSetExtension = s.extensions.create(SourceSetExtension.EXTENSION_NAME, SourceSetExtension::class.java, project.objects)
                sourceSetExtension.libraries.all { lib ->
                    val libOutputs = project.files(lib.output).builtBy(jextractTask)
                    s.java.srcDir(libOutputs)
                    s.resources.srcDir(libOutputs)
                    s.compileClasspath += libOutputs
                    s.runtimeClasspath += libOutputs
                }
            }
        }
    }

    companion object {
        const val PLUGIN_NAME = "de.infolektuell.jextract"
    }
}
