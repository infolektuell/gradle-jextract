package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension
import de.infolektuell.gradle.jextract.service.JextractStore
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask
import de.infolektuell.gradle.jextract.tasks.JextractDumpIncludesTask
import de.infolektuell.gradle.jextract.tasks.JextractGenerateTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.gradle.sharedServices.registerIfAbsent(JextractStore.SERVICE_NAME, JextractStore::class.java) { s ->
            s.maxParallelUsages.convention(1)
            s.parameters.cacheDir.convention(project.rootProject.layout.projectDirectory.dir(".gradle/jextract"))
        }

        val extension = project.extensions.create(JextractExtension.EXTENSION_NAME, JextractExtension::class.java)
        val defaultInstallation = project.objects.newInstance(JextractBaseTask.RemoteJextractInstallation::class.java).apply {
            javaLanguageVersion.convention(JavaLanguageVersion.of(Jvm.current().javaVersionMajor ?: 22))
        }
        extension.installation.convention(defaultInstallation)
        extension.output.convention(project.layout.buildDirectory.dir("generated/sources/jextract"))
        extension.generateSourceFiles.convention(false)

        extension.libraries.configureEach { lib ->
            lib.useSystemLoadLibrary.convention(false)
            lib.output.convention(extension.output.dir(lib.name))
            lib.generateSourceFiles.convention(extension.generateSourceFiles)
        }

        val jextractGenerateTasks = mutableMapOf<String, TaskProvider<JextractGenerateTask>>()
        extension.libraries.all { lib ->
            val generateTaskProvider = project.tasks.register("${lib.name}JextractGenerateBindings", JextractGenerateTask::class.java) { task ->
                task.description = "Uses Jextract to generate Java bindings for the ${lib.name} native library"
                task.installation.set(extension.installation)
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
                task.description = "Uses Jextract to dump all includes of the ${lib.name} native library into an arg file"
                task.installation.set(extension.installation)
                task.header.set(lib.header)
                task.includes.set(lib.includes)
                task.argFile.set(project.layout.buildDirectory.file("reports/jextract/${lib.name}-includes.txt"))
            }
        }

        project.pluginManager.withPlugin("java") {
            project.extensions.findByType(JavaPluginExtension::class.java)?.let { defaultInstallation.javaLanguageVersion.convention(it.toolchain.languageVersion) }
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
    }

    companion object {
        const val PLUGIN_NAME = "de.infolektuell.jextract"
    }
}
