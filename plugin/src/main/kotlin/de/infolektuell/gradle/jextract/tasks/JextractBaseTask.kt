package de.infolektuell.gradle.jextract.tasks

import de.infolektuell.gradle.jextract.service.JextractStore
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*

/** Offers common properties for Jextract-related tasks, does nothing itself */
abstract class JextractBaseTask : DefaultTask() {
    /** A build service to run Jextract commands */
    @get:ServiceReference(JextractStore.SERVICE_NAME)
    protected abstract val jextractStore: Property<JextractStore>

    /** Configures which Jextract installation should be used */
    @get:Nested
    abstract val installation: Property<JextractInstallation>

    /** All directories to append to the list of include search paths */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ListProperty<Directory>

    /** The library header file to generate bindings for */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val header: RegularFileProperty
}
