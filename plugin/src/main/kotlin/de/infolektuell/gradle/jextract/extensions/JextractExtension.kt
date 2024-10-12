package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet
import javax.inject.Inject

abstract class JextractExtension @Inject constructor(objects: ObjectFactory) {
    /** Configures the Jextract tool used by the plugin */
    @get:Nested
    abstract val generator: GeneratorHandler
    /** Configures the Jextract tool used by the plugin */
    fun generator(action: Action<in GeneratorHandler>) {
        action.execute(generator)
    }
    /** The libraries Jextract should generate bindings for */
    val libraries: NamedDomainObjectContainer<LibraryHandler> = objects.domainObjectContainer(LibraryHandler::class.java)
    /** Configures the libraries Jextract should generate bindings for */
    fun libraries(action: Action<in NamedDomainObjectContainer<LibraryHandler>>) {
        action.execute(libraries)
    }
    /** The generated Java sources will be added to this source set (main by default) */
    abstract val sourceSet: Property<SourceSet>
    companion object {
        const val EXTENSION_NAME = "jextract"
    }
}
