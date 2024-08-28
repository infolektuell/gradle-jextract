package de.infolektuell.jextract.tasks
import org.gradle.nativeplatform.platform.Architecture
import org.gradle.nativeplatform.platform.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.net.URI

data class JextractDistribution(val windows_x64: Resource, val linux_x64: Resource, val mac_x64: Resource, val mac_aarch64: Resource? = null) {
    fun selectResource(): Resource {
        if (currentOs.isLinux) {
            return linux_x64
        }
        else if (currentOs.isWindows) {
            return windows_x64
        }
        else if (currentOs.isMacOsX) {
            if (currentArch.name == "x86-64") {
                return mac_x64
            }
            return mac_aarch64 ?: mac_x64
        }
        return windows_x64
    }
    companion object {
        val currentOs: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
        val currentArch: Architecture = DefaultNativePlatform.getCurrentArchitecture()
    }
}

data class Resource(val address: String, val checksum: String) {
    val uri: URI = URI.create(address)
}
