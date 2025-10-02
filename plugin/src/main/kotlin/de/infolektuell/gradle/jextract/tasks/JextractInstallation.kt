package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.jvm.toolchain.JavaLanguageVersion

/** Configuration of a Jextract installation that can be used by this task */
sealed interface JextractInstallation

/** Configuration of a downloadable Jextract version */
interface RemoteJextractInstallation : JextractInstallation {
    /** The [Java version][javaLanguageVersion] the code should be generated for */
    @get:Input
    val javaLanguageVersion: Property<JavaLanguageVersion>
}

/** Configuration of a local Jextract installation */
interface LocalJextractInstallation : JextractInstallation {
    /** A directory containing a JExtract installation */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val location: DirectoryProperty
}

