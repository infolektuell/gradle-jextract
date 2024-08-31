package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class JextractExtension @Inject constructor(objects: ObjectFactory) {
    /** Configures the Jextract tool used by the plugin */
    @get:Nested
    abstract val generator: de.infolektuell.gradle.jextract.extensions.GeneratorHandler
    fun generator(action: Action<in de.infolektuell.gradle.jextract.extensions.GeneratorHandler>) {
        action.execute(generator)
    }
    abstract val targetPackage: Property<String>
    abstract val useSystemLoadLibrary: Property<Boolean>
    val libraries: NamedDomainObjectContainer<de.infolektuell.gradle.jextract.extensions.LibraryHandler> = objects.domainObjectContainer(
        de.infolektuell.gradle.jextract.extensions.LibraryHandler::class.java)
    fun libraries(action: Action<in NamedDomainObjectContainer<de.infolektuell.gradle.jextract.extensions.LibraryHandler>>) {
        action.execute(libraries)
    }
}
