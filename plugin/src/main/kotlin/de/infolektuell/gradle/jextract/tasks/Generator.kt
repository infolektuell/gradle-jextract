package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

abstract class Generator {
    @get:Input
    abstract val version: Property<Int>
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val location: DirectoryProperty
    fun findExecutable(): Provider<String> = location.map { g ->
        g.asFileTree.matching { spec ->
            val fileName = if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) "jextract.bat" else "jextract"
            spec.include("**/bin/$fileName")
        }.singleFile.absolutePath
    }
}
