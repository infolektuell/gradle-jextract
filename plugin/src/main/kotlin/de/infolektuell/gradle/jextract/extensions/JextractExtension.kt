package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
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
    /** specify the directory to place generated files of all libraries by default */
    abstract val output: DirectoryProperty
    /** Generate source files instead of class files for all libraries where not set explicitly (Jextract 21 and below) */
    abstract val generateSourceFiles: Property<Boolean>
    companion object {
        const val EXTENSION_NAME = "jextract"
    }
}
