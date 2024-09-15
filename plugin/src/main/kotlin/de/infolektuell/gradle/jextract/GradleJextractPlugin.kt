package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.download.tasks.DownloadTask
import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.extensions.ResourceHandler
import de.infolektuell.gradle.jextract.extensions.WhitelistHandler
import de.infolektuell.gradle.jextract.tasks.ExtractTask
import de.infolektuell.gradle.jextract.tasks.GenerateBindingsTask
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("de.infolektuell.download")
        val extension = project.extensions.create(JextractExtension.EXTENSION_NAME, JextractExtension::class.java)
        extension.generator.distribution.registerPlatforms()

        val userOutput = project.layout.projectDirectory.dir(project.gradle.gradleUserHomeDir.absolutePath)
        val resource: ResourceHandler = extension.generator.distribution.currentResource()
        val downloadTask = project.tasks.register("downloadJextract", DownloadTask::class.java) { task ->
            task.group = "download"
            task.description = "Downloads Jextract for ${resource.name} platform"
            task.source.set(resource.url)
            task.target.set(userOutput.dir("downloads").file(resource.url.map { it.path.replaceBeforeLast("/", "").trim('/') }))
            task.integrity.put(resource.integrity.algorithm.get(), resource.integrity.checksum)
        }

        val extractTask = project.tasks.register("extract", ExtractTask::class.java) { task ->
            task.source.set(downloadTask.get().target)
            task.target.set(userOutput.dir("jextract").dir(resource.integrity.checksum.map { it.substring(0, 8) }))
        }

        extension.libraries.all { lib ->
            lib.useSystemLoadLibrary.convention(false)
            project.tasks.register("${lib.name}Jextract", GenerateBindingsTask::class.java) { task ->
                task.run {
                    group = "Build"
                    outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/jextract/main/java"))
                    description = "Generates bindings for the ${lib.name} library using Jextract"
                    if (extension.generator.local.isPresent){
                        generator.set(extension.generator.local)
                    } else {
                        generator.set(extractTask.get().target)
                    }
                    header.set(lib.header)
                    definedMacros.set(lib.definedMacros)
                    whitelist.connect(lib.whitelist)
                    targetPackage.set(lib.targetPackage)
                    headerClassName.set(lib.headerClassName)
                    includes.from(lib.includes.sourceDirectories)
                    libraries.set(lib.libraries)
                    useSystemLoadLibrary.set(lib.useSystemLoadLibrary)
                }
            }
        }
        project.plugins.withType(JavaPlugin::class.java) { _ ->
            val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            val version = javaExtension.toolchain.languageVersion.orElse(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
            extension.generator.javaVersion(version.get())
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME) { main ->
                val generatorTasks = project.tasks.withType(GenerateBindingsTask::class.java)
                val generatedFiles = project.files(generatorTasks)
                main.java.srcDir(generatorTasks)
                main.compileClasspath += generatedFiles
                main.runtimeClasspath += generatedFiles
            }
        }
    }

    private fun NamedDomainObjectContainer<ResourceHandler>.registerPlatforms() {
        register("linux_aarch64")
        register("linux_x64")
        register("mac_aarch64")
        register("mac_x64")
        register("windows_aarch64")
        register("windows_x64")
    }
    private fun NamedDomainObjectContainer<ResourceHandler>.currentResource(): ResourceHandler {
        val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
        val currentArch = DefaultNativePlatform.getCurrentArchitecture()
        return if (currentOs.isLinux) {
            if (currentArch.isArm) named("linux_aarch64").orElse(getByName("linux_x64"))
            getByName("linux_x64")
        }
        else if (currentOs.isMacOsX) {
            if (currentArch.isArm) named("mac_aarch64").orElse(getByName("mac_x64"))
            getByName("mac_x64")
        }
        else if (currentOs.isWindows) {
            if (currentArch.isArm) named("windows_aarch64").orElse(getByName("windows_x64"))
            getByName("windows_x64")
        }
        else {
            getByName("windows_x64")
        }
    }
    private fun MapProperty<String, List<String>>.connect(whitelist: WhitelistHandler) {
        put("constant", whitelist.constants)
        put("function", whitelist.functions)
        put("struct", whitelist.structs)
        put("typedef", whitelist.typedefs)
        put("union", whitelist.unions)
        put("var", whitelist.variables)
    }
}
