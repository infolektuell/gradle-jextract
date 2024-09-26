package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.GradleException
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.net.URI
import java.util.Properties

abstract class DefaultJextractResolver : JextractResolver {
    data class Resource(override val url: URI, override val checksum: String, override val algorithm: String) : JextractResolver.Resource {
        override val fileName = url.path.replaceBeforeLast("/", "").trim('/')
        constructor(url: String, checksum: String) : this(URI.create(url), checksum, "SHA-256")
    }
    override fun obtain(): JextractResolver.Resource {
        val data = Properties().apply {
            object {}.javaClass.getResourceAsStream("/jextract.properties")?.use { load(it) }
        }
        val version = parameters.javaLanguageVersion.get().asInt()
        if (version < 19) throw GradleException("Jextract requires at least Java 19")
        val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
        val currentArch = DefaultNativePlatform.getCurrentArchitecture()
        val osKey = if (currentOs.isLinux) {
            "linux"
        } else if (currentOs.isMacOsX) {
            "mac"
        } else {
            "windows"
        }
        val archKey = if (currentArch.isArm) {
            "aarch64"
        } else {
            "x64"
        }
        val url = data.getProperty("jextract.$version.$osKey.$archKey.url") ?: data.getProperty("jextract.$version.$osKey.x64.url")
        val checksum = data.getProperty("jextract.$version.$osKey.$archKey.sha-256") ?: data.getProperty("jextract.$version.$osKey.x64.sha-256")
        return Resource(url, checksum)
    }
}
