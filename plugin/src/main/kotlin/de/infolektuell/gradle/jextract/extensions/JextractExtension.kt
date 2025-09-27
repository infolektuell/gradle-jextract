package de.infolektuell.gradle.jextract.extensions

import de.infolektuell.gradle.jextract.tasks.JextractBaseTask.*
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLanguageVersion
import javax.inject.Inject

abstract class JextractExtension @Inject constructor(private val objects: ObjectFactory) {
    /** Configuration of a Jextract installation to be used by the plugin */
    abstract val installation: Property<JextractInstallation>

    /** Configures a local Jextract installation to be used by the plugin */
    fun download(action: Action<in RemoteJextractInstallation>) {
        installation.set(objects.newInstance(RemoteJextractInstallation::class.java, action))
    }
    fun download(javaLanguageVersion: JavaLanguageVersion) {
        download {
            it.javaLanguageVersion.convention(javaLanguageVersion)
        }
    }

    /** Configures a local Jextract installation to be used by the plugin */
    fun local(action: Action<in LocalJextractInstallation>) {
        installation.set(objects.newInstance(LocalJextractInstallation::class.java, action))
    }

    /** Configures a local Jextract installation to be used by the plugin */
    fun local(location: DirectoryProperty) {
        local {
            it.location.convention(location)
        }
    }

    /** Configures a local Jextract installation to be used by the plugin */
    fun local(location: Directory) {
        local {
            it.location.convention(location)
        }
    }

    /** A [properties][java.util.Properties] file containing the remote locations where to download the Jextract distributions */
    abstract val distributions: RegularFileProperty

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
