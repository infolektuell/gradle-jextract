package de.infolektuell.gradle.jextract.service

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.net.URI
import java.util.Properties
import kotlin.math.max
import kotlin.math.min

class JextractDataStore {
    enum class OS {
        LINUX, MAC, WINDOWS;
        override fun toString() = this.name.lowercase()
    }
    enum class Arch {
        AARCH64, X64;
        override fun toString() = this.name.lowercase()
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
    fun version(javaLanguageVersion: Int) = max(min(javaLanguageVersion, 22), 19)
    fun resource(javaLanguageVersion: Int): DownloadClient.Resource {
        val version = version(javaLanguageVersion)
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.${Arch.X64}.url")
        val checksum: String = data.getProperty("jextract.$version.$os.$arch.sha-256") ?: data.getProperty("jextract.$version.$os.${Arch.X64}.sha-256")
        return DownloadClient.Resource(URI.create(url), checksum)
    }
}
