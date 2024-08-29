package de.infolektuell.jextract.extensions
import org.gradle.api.provider.Property

abstract class IntegrityHandler {
    abstract val checksum: Property<String>
    fun checksum(value: String) = checksum.set(value)
    abstract val algorithm: Property<String>
    fun algorithm(value: String) = algorithm.set(value)
}
