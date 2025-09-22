package de.infolektuell.gradle.jextract.service

import de.infolektuell.gradle.jextract.service.DownloadClient.Resource
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.util.*
import kotlin.math.max
import kotlin.math.min

class JextractDataStore {
    fun loadDistributions(file: File): Properties {
        return Properties(defaultDistributions()).apply {
            load(file.reader(Charset.defaultCharset()))
        }
    }
    fun defaultDistributions(): Properties? {
        return object {}.javaClass.getResourceAsStream("/jextract.properties")?.use {
            Properties().apply { load(it) }
        }
    }
    fun resource(data: Properties, version: Int): Resource {
        val version = clamp(version, 19, 22)
        val os = currentOs()
        val arch = currentArch()
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.x64.url")
        val checksum: String = data.getProperty("jextract.$version.$os.$arch.sha-256") ?: data.getProperty("jextract.$version.$os.x64.sha-256")
        return Resource(URI.create(url), checksum)
    }
    private fun clamp(value: Int, lower: Int, upper: Int) = max(min(value, upper), lower)
    private fun currentArch(): String {
        val arch = DefaultNativePlatform.getCurrentArchitecture()
        return if (arch.isArm64) {
            "aarch64"
        } else {
            "x64"
        }
    }
    private fun currentOs(): String {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        return if (os.isMacOsX) {
            "mac"
        } else if (os.isWindows) {
            "windows"
        } else {
            "linux"
        }
    }
}
