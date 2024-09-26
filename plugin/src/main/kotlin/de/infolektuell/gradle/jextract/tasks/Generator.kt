package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.io.File
import javax.inject.Inject

abstract class Generator @Inject constructor(objects: ObjectFactory, private val execOperations: ExecOperations) {
    @get:InputDirectory
    val location: DirectoryProperty = objects.directoryProperty()
    @get:Internal
    val executable: Provider<File> = location.map { g ->
        g.asFileTree.matching { spec ->
            val fileName = if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) "jextract.bat" else "jextract"
            spec.include("**/bin/$fileName")
        }.singleFile
    }
    fun execute(action: Action<in ExecSpec>) {
        execOperations.exec { spec ->
            spec.executable(executable.get())
            action.execute(spec)
        }
    }
}
