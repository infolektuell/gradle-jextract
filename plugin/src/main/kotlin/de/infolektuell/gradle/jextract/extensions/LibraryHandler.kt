package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class LibraryHandler @Inject constructor(objects: ObjectFactory) : Named {
    abstract val header: RegularFileProperty
    val includes: SourceDirectorySet = objects.sourceDirectorySet("jextractIncludes", "Jextract Includes").apply {
        include("*.h")
    }
    abstract val definedMacros: ListProperty<String>
    abstract val targetPackage: Property<String>
    abstract val headerClassName: Property<String>
    @get:Nested
    abstract val whitelist: WhitelistHandler
    fun whitelist(action: Action<in WhitelistHandler>) {
        action.execute(whitelist)
    }
    abstract val libraries: ListProperty<String>
    abstract val useSystemLoadLibrary: Property<Boolean>
}
