package de.infolektuell.gradle.jextract

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.Serializable
import java.net.URI
import java.util.*

class JextractDataStore {
    enum class OS {
        LINUX, MAC, WINDOWS;
        override fun toString() = this.name.lowercase()
    }
    enum class Arch {
        AARCH64, X64;
        override fun toString() = this.name.lowercase()
    }
    data class Resource(val version: Int, val url: URI, val checksum: String, val algorithm: String = "SHA-256") : Serializable {
        val filename: String get() = url.path.replaceBeforeLast('/', "").trim('/')
        companion object {
            const val serialVersionUID: Long = 0
        }
    }
    val data = Properties().apply {
        object {}.javaClass.getResourceAsStream("/jextract.properties")?.use { load(it) }
    }
    val os: OS get() {
        val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
        return if (currentOs.isLinux) {
            OS.LINUX
        } else if (currentOs.isMacOsX) {
            OS.MAC
        } else {
            OS.WINDOWS
        }
    }
    val arch: Arch get() {
        val currentArch = DefaultNativePlatform.getCurrentArchitecture()
        return if (currentArch.isArm) {
            Arch.AARCH64
        } else {
            Arch.X64
        }
    }
    fun resource(javaLanguageVersion: Int): Resource {
        val version = kotlin.math.max(kotlin.math.min(javaLanguageVersion, 22), 19)
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.${Arch.X64}.url")
        val checksum: String = data.getProperty("jextract.$version.$os.$arch.sha-256") ?: data.getProperty("jextract.$version.$os.${Arch.X64}.sha-256")
        return Resource(version, URI.create(url), checksum)
    }
}
