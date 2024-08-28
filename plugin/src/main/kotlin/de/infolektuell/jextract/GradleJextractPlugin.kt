package de.infolektuell.jextract

import de.infolektuell.jextract.extensions.JextractExtension.Companion.jextract
import de.infolektuell.jextract.tasks.DownloadTask
import de.infolektuell.jextract.tasks.GenerateBindingsTask
import de.infolektuell.jextract.tasks.JextractDistribution
import de.infolektuell.jextract.tasks.Resource
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

abstract class GradleJextractPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.jextract()
        val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)
        val downloadJextract = javaExtension?.let { setupDownload(project, it) }

        extension.libraries.all { lib ->
            val generateBindingsTask = project.tasks.register(lib.name, GenerateBindingsTask::class.java) { task ->
                task.run {
                    group = "code generation"
                    description = "Generates bindings for the ${lib.name} library"
                    if (extension.generator.isPresent) {
                        generator.set(extension.generator)
                    } else {
                        downloadJextract?.let { dt ->
                            dependsOn(dt)
                            generator.set(dt.get().outputDirectory)
                        }
                    }
                    header.set(lib.header)
                    targetPackage.set(lib.targetPackage.orElse(extension.targetPackage))
                    headerClassName.set(lib.headerClassName)
                    useSystemLoadLibrary.convention(false).set(lib.useSystemLoadLibrary.orElse(extension.useSystemLoadLibrary))
                    output.convention(project.layout.buildDirectory.dir("generated/sources/jextract/${lib.name}/main/java"))
                }
            }
            javaExtension?.run {
                sourceSets.getByName("main").java.srcDir(generateBindingsTask)
            }
        }
    }

    private fun setupDownload(project: Project, javaExtension: JavaPluginExtension): TaskProvider<DownloadTask> {
        val version: JavaLanguageVersion = javaExtension.toolchain.languageVersion.orElse(JavaLanguageVersion.of(Jvm.current().javaVersionMajor ?: 22)).get()
        val distribution = distributions[version] ?: throw GradleException("Your java toolchain is too old for Jextract")
        val resource = distribution.selectResource()
        return project.tasks.register("downloadJextract", DownloadTask::class.java) { task ->
            task.src.set(resource.uri)
            task.algorithm.set("SHA-256")
            task.checksum.set(resource.checksum)
            task.outputDirectory.set(project.gradle.gradleUserHomeDir.resolve("jextract/$version/"))
        }
    }

    companion object {
        val distributions = mapOf<JavaLanguageVersion, JextractDistribution>(
            JavaLanguageVersion.of(22) to JextractDistribution(
                linux_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_linux-x64_bin.tar.gz",
                    "53d66299cda8d079aeff42b2cc765314e44b384f3e0ec2a7eb994bae62b4b728"
                ),
                mac_aarch64 = Resource(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_macos-aarch64_bin.tar.gz",
                    "2a4411c32aedb064c3e432eb8a2791e6e60fea452330c71386f6573dc4c9c850"
                ),
                mac_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_macos-x64_bin.tar.gz",
                    "0f65d480a1713d73c179e91f3ab6b9553c22694cd1a9f7936ffa8ca351d12390"
                ),
                windows_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_windows-x64_bin.tar.gz",
                    "f51d7b79dac50dbe1827f73ea4569d7565657f107bdb41f9dc90057a1106b267"
                ),
            ),
            JavaLanguageVersion.of(21) to JextractDistribution(
                linux_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_linux-x64_bin.tar.gz",
                    "83626610b1b074bfe4985bd825d8ba44d906a30b24c42d971b6ac836c7eb0671"
                ),
                mac_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_macos-x64_bin.tar.gz",
                    "6183f3d079ed531cc5a332e6d86c0abfbc5d001f1e85f721ebc5232204c987a2"
                ),
                windows_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_windows-x64_bin.tar.gz",
                    "30a4723f4eaa506b926d3d8d368e5b00e2774be7b5df326a7f779bbef48de69f"
                ),
            ),
            JavaLanguageVersion.of(20) to JextractDistribution(
                linux_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_linux-x64_bin.tar.gz",
                    "fced495b34d776f91f65a4f72023f2c3de0c9e8c3787157288baf0d82d1ec1f2"
                ),
                mac_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_macos-x64_bin.tar.gz",
                    "84e3dcd6674ad486b01b5fb2cf7359adf5a1d5112d13de2321882bf1ec1dd904"
                ),
                windows_x64 = Resource(
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_windows-x64_bin.tar.gz",
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_windows-x64_bin.tar.gz.sha256"
                ),
            )
        )
    }
}
