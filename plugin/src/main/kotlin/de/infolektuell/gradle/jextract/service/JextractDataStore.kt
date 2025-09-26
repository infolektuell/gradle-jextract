package de.infolektuell.gradle.jextract.service

import de.infolektuell.gradle.jextract.service.DownloadClient.Resource
import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.util.*
import kotlin.math.max
import kotlin.math.min

class JextractDataStore {
    private val arch: String by lazy { currentArch() }
    private val os: String by lazy { currentOs() }

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
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.x64.url")
        val checksum: String = data.getProperty("jextract.$version.$os.$arch.sha-256") ?: data.getProperty("jextract.$version.$os.x64.sha-256")
        return Resource(URI.create(url), checksum)
    }
    @Suppress("SameParameterValue")
    private fun clamp(value: Int, lower: Int, upper: Int) = max(min(value, upper), lower)
    private fun currentArch(): String {
        return System.getProperty("os.arch")
    }
    private fun currentOs(): String {
        return when(System.getProperty("os.name")) {
            "Windows" -> "windows"
            "Mac OS X" -> "mac"
            "Linux" -> "linux"
            else -> "linux"
        }
    }
}
