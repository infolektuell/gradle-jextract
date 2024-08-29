package de.infolektuell.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class JextractExtension @Inject constructor(objects: ObjectFactory) {
    /** Configures the Jextract tool used by the plugin */
    @get:Nested
    abstract val generator: GeneratorHandler
    fun generator(action: Action<in GeneratorHandler>) {
        action.execute(generator)
    }
    abstract val targetPackage: Property<String>
    abstract val useSystemLoadLibrary: Property<Boolean>
    val libraries: NamedDomainObjectContainer<LibraryHandler> = objects.domainObjectContainer(LibraryHandler::class.java)
    fun libraries(action: Action<in NamedDomainObjectContainer<LibraryHandler>>) {
        action.execute(libraries)
    }
}
