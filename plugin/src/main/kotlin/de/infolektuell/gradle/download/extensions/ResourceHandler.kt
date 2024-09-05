package de.infolektuell.gradle.download.extensions

import org.gradle.api.Named
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.net.URI

abstract class ResourceHandler : Named {
    abstract val source: Property<URI>
    abstract val target: RegularFileProperty
    abstract val integrity: MapProperty<String, String>
    fun sha256(checksum: String) = integrity.put("SHA-256", checksum)
    fun sha512(checksum: String) = integrity.put("SHA-512", checksum)
    fun md5(checksum: String) = integrity.put("MD5", checksum)
}
