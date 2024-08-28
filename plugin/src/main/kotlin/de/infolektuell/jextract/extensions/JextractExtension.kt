package de.infolektuell.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class JextractExtension @Inject constructor(objects: ObjectFactory) {
    abstract val generator: DirectoryProperty
    abstract val targetPackage: Property<String>
    abstract val useSystemLoadLibrary: Property<Boolean>
    val libraries: NamedDomainObjectContainer<LibraryHandler> = objects.domainObjectContainer(LibraryHandler::class.java)
    fun libraries(action: Action<in NamedDomainObjectContainer<LibraryHandler>>) {
        action.execute(libraries)
    }
    companion object {
        fun Project.jextract(): JextractExtension = extensions.create("jextract", JextractExtension::class.java)
    }
}
