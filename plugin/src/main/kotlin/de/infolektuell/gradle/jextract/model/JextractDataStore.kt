package de.infolektuell.gradle.jextract.model

import de.infolektuell.gradle.jextract.model.DownloadClient.Resource
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.math.max
import kotlin.math.min

/** A service class to query data for downloading Jextract, depending on the current platform and a given target JVM version */
class JextractDataStore {
    private val distributionCache = mutableMapOf<Path, Properties>()
    private val defaultDistributions: Properties by lazy { loadDefaultDistributions() }
    private val arch: String by lazy { currentArch() }
    private val os: String by lazy { currentOs() }

    /** Returns a downloadable resource for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data */
    fun resource(version: Int, file: Path? = null) = resource(version, loadDistributions(file))
    /** Returns an archive filename for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data */
    fun filename(version: Int, file: Path? = null) = filename(version, loadDistributions(file))

    private fun resource(version: Int, data: Properties): Resource {
        val version = clamp(version, 19, 22)
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.x64.url")
        val checksum: String = data.getProperty("jextract.$version.$os.$arch.sha-256") ?: data.getProperty("jextract.$version.$os.x64.sha-256")
        return Resource(URI.create(url), checksum)
    }

    private fun filename(version: Int, data: Properties): String {
        val version = clamp(version, 19, 22)
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.x64.url")
        return url.replaceBeforeLast('/', "").trim('/')
    }

    private fun loadDistributions(file: Path? = null): Properties {
        if (file == null) return defaultDistributions
        return distributionCache.computeIfAbsent(file) { k ->
            Properties(defaultDistributions).apply {
                load(Files.newBufferedReader(k))
            }
        }
    }

    private fun loadDefaultDistributions(): Properties {
        return Properties().apply {
            object {}.javaClass.getResourceAsStream("/jextract.properties")?.use{
                load(it)
            }
        }
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
