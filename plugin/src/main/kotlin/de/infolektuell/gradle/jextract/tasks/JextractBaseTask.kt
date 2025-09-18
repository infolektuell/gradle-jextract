package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File

abstract class JextractBaseTask : DefaultTask() {
    @get:Input
    abstract val version: Property<Int>
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distribution: DirectoryProperty
    @get:Internal
    protected val executable: Provider<File> = distribution.map { g ->
        g.asFileTree.matching { spec ->
            val fileName = if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) "jextract.bat" else "jextract"
            spec.include("**/bin/$fileName")
        }.singleFile
    }
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ListProperty<Directory>
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val header: RegularFileProperty
}
