package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.net.URI
import javax.inject.Inject

abstract class GeneratorHandler @Inject constructor(objects: ObjectFactory) {
    /** Contains platform-specific locations where JExtract can be downloaded */
    val distribution: NamedDomainObjectContainer<ResourceHandler> = objects.domainObjectContainer(ResourceHandler::class.java)
    /** Configures platform-specific locations where JExtract can be downloaded */
    fun distribution(action: Action<in NamedDomainObjectContainer<ResourceHandler>>) {
        action.execute(distribution)
    }
    /** A directory containing a JExtract installation */
    abstract val local: DirectoryProperty

    /** Sets sensible defaults to download Jextract for a specific Java version */
    fun javaVersion(version: JavaLanguageVersion) {
        when(version) {
            JavaLanguageVersion.of(22) -> {
                distribution.getByName("linux_x64") { it.setConventions(
                        "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_linux-x64_bin.tar.gz",
                        "53d66299cda8d079aeff42b2cc765314e44b384f3e0ec2a7eb994bae62b4b728",
                    )}
                distribution.getByName("mac_aarch64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_macos-aarch64_bin.tar.gz",
                    "2a4411c32aedb064c3e432eb8a2791e6e60fea452330c71386f6573dc4c9c850",
                )}
                distribution.getByName("mac_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_macos-x64_bin.tar.gz",
                    "0f65d480a1713d73c179e91f3ab6b9553c22694cd1a9f7936ffa8ca351d12390",
                )}
                distribution.getByName("windows_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/22/5/openjdk-22-jextract+5-33_windows-x64_bin.tar.gz",
                    "f51d7b79dac50dbe1827f73ea4569d7565657f107bdb41f9dc90057a1106b267",
                )}
            }
            JavaLanguageVersion.of(21) -> {
                distribution.getByName("linux_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_linux-x64_bin.tar.gz",
                    "83626610b1b074bfe4985bd825d8ba44d906a30b24c42d971b6ac836c7eb0671",
                )}
                distribution.getByName("mac_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_macos-x64_bin.tar.gz",
                    "6183f3d079ed531cc5a332e6d86c0abfbc5d001f1e85f721ebc5232204c987a2",
                )}
                distribution.getByName("windows_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_windows-x64_bin.tar.gz",
                    "30a4723f4eaa506b926d3d8d368e5b00e2774be7b5df326a7f779bbef48de69f",
                )}
            }
            JavaLanguageVersion.of(20) -> {
                distribution.getByName("linux_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_linux-x64_bin.tar.gz",
                    "fced495b34d776f91f65a4f72023f2c3de0c9e8c3787157288baf0d82d1ec1f2",
                )}
                distribution.getByName("mac_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_macos-x64_bin.tar.gz",
                    "84e3dcd6674ad486b01b5fb2cf7359adf5a1d5112d13de2321882bf1ec1dd904",
                )}
                distribution.getByName("windows_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/20/1/openjdk-20-jextract+1-2_windows-x64_bin.tar.gz",
                    "9ad31b338ff8b94834869e6d865e2b986efcdaa0d76e28d4054f8b291b74019f"
                )}
            }
            JavaLanguageVersion.of(19) -> {
                distribution.getByName("linux_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/19/2/openjdk-19-jextract+2-3_linux-x64_bin.tar.gz",
                    "5f97e63ed608e7a68e7995b8038e225b8beb2e9fb67216ba2bcd711453a6bb51",
                )}
                distribution.getByName("mac_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/19/2/openjdk-19-jextract+2-3_macos-x64_bin.tar.gz",
                    "f2672332b5a4a8f07464472f74060e8fad9ab86c77bf3d4def8fb0fc4c6b4510",
                )}
                distribution.getByName("windows_x64") { it.setConventions(
                    "https://download.java.net/java/early_access/jextract/19/2/openjdk-19-jextract+2-3_windows-x64_bin.tar.gz",
                    "770723ae81a2ff48bfba6ec83c6fcbff6975a4dbb635b49bd5c3b7c042aefea2"
                )}
            }
            else -> {
                throw GradleException("This plugin does not support conventions for supplied Java version. Please select a version between 19 and 22.")
            }
        }
    }
    private fun ResourceHandler.setConventions(src: String, checksum: String) {
        url.convention(URI.create(src))
        integrity.checksum.convention(checksum)
        integrity.algorithm.convention("SHA-256")
    }
}
