package de.infolektuell.jextract.extensions

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class LibraryHandler @Inject constructor(private val name: String) : Named {
    override fun getName() = name
    abstract val header: RegularFileProperty
    abstract val targetPackage: Property<String>
    abstract val headerClassName: Property<String>
    @get:Nested
    abstract val whitelist: WhitelistHandler
    fun whitelist(action: Action<in WhitelistHandler>) {
        action.execute(whitelist)
    }
    abstract val libraries: ListProperty<RegularFile>
    abstract val useSystemLoadLibrary: Property<Boolean>
}
