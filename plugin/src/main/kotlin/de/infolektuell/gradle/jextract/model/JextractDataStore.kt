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

    private val platform = Platform.getCurrentPlatform()
    private val os get() = platform.operatingSystem.name.lowercase()
    private val arch get() = platform.architecture.name.lowercase()
    private val distributionCache = mutableMapOf<Path, Properties>()
    private val defaultDistributions: Properties by lazy { loadDefaultDistributions() }
    val executableFilename: String by lazy { if (platform.isWindows) "jextract.bat" else "jextract" }

    /** Returns the matching Jextract version for a given [Java major version][javaVersion] */
    @Suppress("SameParameterValue")
    fun version(javaVersion: Int, lower: Int = 19, upper: Int = 22) = max(min(javaVersion, upper), lower)

    /** Returns a downloadable resource for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data */
    fun resource(version: Int, file: Path? = null) = resource(version, loadDistributions(file))

    /** Returns an archive filename for the specified Jextract [version],  using the data from an optional [file], falls back to the official distribution data */
    fun filename(version: Int, file: Path? = null) = filename(version, loadDistributions(file))

    private fun resource(javaVersion: Int, data: Properties): Resource {
        val version = version(javaVersion)
        val url: String = data.getProperty("jextract.$version.$os.$arch.url") ?: data.getProperty("jextract.$version.$os.x64.url")
        val checksum: String = data.getProperty("jextract.$version.$os.$arch.sha-256") ?: data.getProperty("jextract.$version.$os.x64.sha-256")
        return Resource(URI.create(url), checksum)
    }

    private fun filename(javaVersion: Int, data: Properties): String {
        val version = version(javaVersion)
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
}
