package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.JextractExtension
import de.infolektuell.gradle.jextract.extensions.ResourceHandler
import de.infolektuell.gradle.jextract.extensions.WhitelistHandler
import de.infolektuell.gradle.jextract.tasks.DownloadTask
import de.infolektuell.gradle.jextract.tasks.GenerateBindingsTask
import de.infolektuell.gradle.jextract.services.JextractDownloadClient
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("jextract", JextractExtension::class.java)
        extension.generator.distribution.registerPlatforms()
        val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
            val version = toolchain.languageVersion.orElse(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
            extension.generator.javaVersion(version.get())
        }

        val userOutput = project.layout.projectDirectory.dir(project.gradle.gradleUserHomeDir.absolutePath)
        project.gradle.sharedServices.registerIfAbsent("download", JextractDownloadClient::class.java) { service ->
            service.parameters.outputDirectory.convention(userOutput.dir("downloads"))
        }
        val downloadJextractTask = project.tasks.register("downloadJextract", DownloadTask::class.java) { task ->
            task.outputDirectory.convention(userOutput.dir("jextract"))
            val resource: ResourceHandler = extension.generator.distribution.currentResource()
            task.description = "Downloads Jextract for ${resource.name} platform"
            task.url.set(resource.url)
            task.checksum.set(resource.integrity.checksum)
            task.algorithm.set(resource.integrity.algorithm)
        }

        val javaCompileTask = project.tasks.named("compileJava", JavaCompile::class.java)
        extension.libraries.all { lib ->
            lib.useSystemLoadLibrary.convention(false)
            val jextractTask = project.tasks.register("${lib.name}Jextract", GenerateBindingsTask::class.java) { task ->
                task.group = "Build"
                task.outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/jextract/main/java"))
                task.description = "Generates bindings for the ${lib.name} library using Jextract"
                if (extension.generator.local.isPresent) {
                    task.generator.set(extension.generator.local)
                } else {
                    task.dependsOn(downloadJextractTask)
                    task.generator.set(downloadJextractTask.get().distributionDirectory)
                }
                task.run {
                    header.set(lib.header)
                    whitelist.connect(lib.whitelist)
                    targetPackage.set(lib.targetPackage)
                    headerClassName.set(lib.headerClassName)
                    includes.set(lib.includes)
                    libraries.set(lib.libraries)
                    useSystemLoadLibrary.set(lib.useSystemLoadLibrary)
                }
                javaExtension?.run {
                    sourceSets.named("main") { s ->
                        s.java.srcDir(task)
                        // Support older Jextract versions that generated some compiled classes
                        val classFiles = project.files(task).filter { spec ->
                            spec.endsWith(".class")
                        }
                        s.compileClasspath += classFiles
                        s.runtimeClasspath += classFiles
                    }
                }
            }
            javaCompileTask.configure { task ->
                task.dependsOn(jextractTask)
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
