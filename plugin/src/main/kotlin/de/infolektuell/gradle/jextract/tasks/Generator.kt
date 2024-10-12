package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import javax.inject.Inject

abstract class Generator @Inject constructor(objects: ObjectFactory) {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val location: DirectoryProperty = objects.directoryProperty()
    @get:Internal
    val executable: Provider<RegularFile> = location.map { g ->
        val file = g.asFileTree.matching { spec ->
            val fileName = if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) "jextract.bat" else "jextract"
            spec.include("**/bin/$fileName")
        }.singleFile
        g.file(file.absolutePath)
    }
}
