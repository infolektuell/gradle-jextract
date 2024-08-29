package de.infolektuell.jextract.extensions
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.net.URI
import javax.inject.Inject

abstract class ResourceHandler : Named {
    abstract val url: Property<URI>
    @get:Nested
    abstract val integrity: IntegrityHandler
    fun integrity(action: Action<in IntegrityHandler>) {
        integrity.algorithm.convention("SHA-256")
        action.execute(integrity)
    }
    fun integrity(checksum: String, algorithm: String = "SHA-256") = integrity.also {
        it.checksum.set(checksum)
        it.algorithm.set(algorithm)
    }
}
