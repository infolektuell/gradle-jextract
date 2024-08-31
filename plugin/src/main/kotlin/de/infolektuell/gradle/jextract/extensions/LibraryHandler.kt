package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

abstract class LibraryHandler : Named {
    abstract val header: RegularFileProperty
    abstract val includes: ListProperty<Directory>
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
