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
    enum class OS {
        WINDOWS, MAC, LINUX;
        override fun toString(): String {
            return when(this) {
                WINDOWS -> "windows"
                MAC -> "mac"
                LINUX -> "linux"
            }
        }
        companion object {
            fun create(value: String): OS {
                return if (value.contains("Windows")) {
                    WINDOWS
                } else if (value.contains("Mac")) {
                    MAC
                } else {
                    LINUX
                }
            }
        }
    }
    enum class Architecture {
        AARCH64, X64;
        override fun toString() = name.lowercase()
        companion object {
            fun create(value: String): Architecture {
                return when (value.lowercase()) {
                    "aarch64" -> AARCH64
                    else -> X64
                }
            }
        }
    }

    private val distributionCache = mutableMapOf<Path, Properties>()
    private val defaultDistributions: Properties by lazy { loadDefaultDistributions() }
    private val arch: Architecture by lazy { Architecture.create(System.getProperty("os.arch")) }
    private val os: OS by lazy { OS.create(System.getProperty("os.name")) }
    val executableFilename: String by lazy { if (os == OS.WINDOWS) "jextract.bat" else "jextract" }

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
