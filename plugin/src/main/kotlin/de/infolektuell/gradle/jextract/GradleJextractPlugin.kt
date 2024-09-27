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
            task.resource.convention(resourceProvider)
            task.target.convention(userOutput.dir("downloads").file(task.resource.map { it.fileName }))
        }

        val extractTask = project.tasks.register("extract", ExtractTask::class.java) { task ->
            task.source.convention(downloadTask.flatMap { it.target })
            task.target.convention(userOutput.dir("jextract").dir(extension.generator.javaLanguageVersion.map { "jextract-${it.asInt()}" }))
        }
        extension.generator.local.convention(extractTask.flatMap { it.target })

        val jextractTask = project.tasks.register("jextract", GenerateBindingsTask::class.java) { task ->
            task.group = "Build"
            task.description = "Generates bindings for all configured libraries"
            task.generator.location.convention(extension.generator.local)
        }
        val dumpIncludesTask = project.tasks.register("dumpIncludes", DumpIncludesTask::class.java) { task ->
            task.group = "documentation"
            task.description = "Generates a dump of all symbols encountered in a header file"
            task.generator.location.convention(extension.generator.local)
        }
        sourceSets?.named("main") { main ->
            main.java.srcDir(jextractTask)
            main.compileClasspath += project.files(jextractTask)
            main.runtimeClasspath += project.files(jextractTask)
        }
        extension.libraries.all { lib ->
            lib.useSystemLoadLibrary.convention(false)
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
                    sources.set(project.layout.buildDirectory.dir("generated/sources/jextract/${lib.name}/main/java"))
                }
                task.libraries.add(config)
            }
            dumpIncludesTask.configure { task ->
                val config = project.objects.newInstance(DumpIncludesTask.LibraryConfig::class.java).apply {
                    header.set(lib.header)
                    argFile.set(project.layout.buildDirectory.file("reports/jextract/${lib.name}-includes.txt"))
                }
                task.libraries.add(config)
            }
        }
    }

    companion object {
        const val PLUGIN_NAME = "de.infolektuell.jextract"
    }
}
