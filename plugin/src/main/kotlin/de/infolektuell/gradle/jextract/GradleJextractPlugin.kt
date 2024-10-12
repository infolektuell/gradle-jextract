package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.tasks.DownloadTask
import de.infolektuell.gradle.jextract.tasks.DumpIncludesTask
import de.infolektuell.gradle.jextract.tasks.ExtractTask
import de.infolektuell.gradle.jextract.tasks.GenerateBindingsTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.*

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(JextractExtension.EXTENSION_NAME, JextractExtension::class.java)
        extension.generator.javaLanguageVersion.convention(JavaLanguageVersion.of(Jvm.current().javaVersionMajor ?: 22))
        project.extensions.findByType(JavaPluginExtension::class.java)?.let { extension.generator.javaLanguageVersion.convention(it.toolchain.languageVersion) }
        project.extensions.findByType(SourceSetContainer::class.java)?.let { extension.sourceSet.convention(it.named("main")) }
        val downloadTask = project.tasks.register("downloadJextract", DownloadTask::class.java) { task ->
            task.description = "Downloads Jextract"
            val data = Properties().apply {
                object {}.javaClass.getResourceAsStream("/jextract.properties")?.use { load(it) }
            }
            val version = extension.generator.javaLanguageVersion.get().asInt()
            if (version < 19) throw GradleException("Jextract requires at least Java 19")
            val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
            val currentArch = DefaultNativePlatform.getCurrentArchitecture()
            val osKey = if (currentOs.isLinux) {
                "linux"
            } else if (currentOs.isMacOsX) {
                "mac"
            } else {
                "windows"
            }
            val archKey = if (currentArch.isArm) {
                "aarch64"
            } else {
                "x64"
            }
            val url = data.getProperty("jextract.$version.$osKey.$archKey.url") ?: data.getProperty("jextract.$version.$osKey.x64.url")
            val checksum = data.getProperty("jextract.$version.$osKey.$archKey.sha-256") ?: data.getProperty("jextract.$version.$osKey.x64.sha-256")
            task.resource.url.convention(project.uri(url))
            task.resource.checksum.convention(checksum)
            task.resource.algorithm.convention("SHA-256")
            task.target.convention(task.resource.fileName.flatMap { project.layout.buildDirectory.file("downloads/$it") })
        }

        val extractTask = project.tasks.register("extract", ExtractTask::class.java) { task ->
            task.source.convention(downloadTask.flatMap { it.target })
            task.target.convention(project.layout.buildDirectory.dir("jextract"))
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
        extension.sourceSet.orNull?.run {
            java.srcDir(jextractTask)
            compileClasspath += project.files(jextractTask)
            runtimeClasspath += project.files(jextractTask)
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
                    includes.set(lib.includes)
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
