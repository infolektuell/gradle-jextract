package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.Serializable
import java.net.URI

interface JextractResolver : ValueSource<JextractResolver.Resource, JextractResolver.Parameters> {
    interface Resource : Serializable {
        val url: URI
        val fileName: String
        val checksum: String
        val algorithm: String
    }
    interface Parameters : ValueSourceParameters {
        val javaLanguageVersion: Property<JavaLanguageVersion>
    }
}
