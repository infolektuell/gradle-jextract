package de.infolektuell.gradle.jextract.tasks

import de.infolektuell.gradle.jextract.service.JextractStore
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion

/** Offers common properties for Jextract-related tasks, does nothing itself */
abstract class JextractBaseTask : DefaultTask() {
    /** Configuration of a Jextract installation that can be used by this task */
    sealed interface JextractInstallation

    /** Configuration of a downloadable Jextract version */
    interface RemoteJextractInstallation : JextractInstallation {
        /** The [Java version][javaLanguageVersion] the code should be generated for */
        @get:Input
        val javaLanguageVersion: Property<JavaLanguageVersion>
        /** A [properties][java.util.Properties] file containing the remote locations where to download the Jextract distributions */
        @get:Optional
        @get:InputFile
        val distributions: RegularFileProperty
    }

    /** Configuration of a local Jextract installation */
    interface LocalJextractInstallation : JextractInstallation {
        /** A directory containing a JExtract installation */
        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val location: DirectoryProperty
    }

    /** A build service to run Jextract commands */
    @get:ServiceReference(JextractStore.SERVICE_NAME)
    protected abstract val jextractStore: Property<JextractStore>

    /** Configures which Jextract installation should be used */
    @get:Nested
    abstract val installation: Property<JextractInstallation>

    /** All directories to be added to the end of the list of include search paths */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ListProperty<Directory>

    /** The library header file to generate bindings for */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val header: RegularFileProperty
}
