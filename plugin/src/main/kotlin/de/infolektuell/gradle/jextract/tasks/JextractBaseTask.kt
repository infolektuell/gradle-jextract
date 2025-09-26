package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject

abstract class JextractBaseTask : DefaultTask() {
    @get:Inject
    protected abstract val execOperations: ExecOperations

    @Deprecated("Version is determined from command line, so this won't be used anymore.")
    @get:Optional
    @get:Input
    abstract val version: Property<Int>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distribution: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ListProperty<Directory>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val header: RegularFileProperty

    private var executable: String? = null
    protected fun execute(action: Action<in ExecSpec>): ExecResult {
        if (executable == null) {
            executable = findExecutable().absolutePath
        }
        return execOperations.exec { spec ->
            spec.executable(executable)
            action.execute(spec)
        }
    }

    protected fun findVersion(): Int? {
        return ByteArrayOutputStream().use { s ->
            execute { spec ->
                spec.args("--version")
                spec.errorOutput = s
            }
            s.toString(Charset.defaultCharset())
                .trim()
                .lines()
                .first()
                .split(" ")
                .last()
                .toIntOrNull()
        }
    }

    private fun findExecutable(): File {
        return  distribution.asFileTree
            .matching { it.include("**/bin/jextract*") }
            .filter { it.canExecute() }
            .singleFile
    }
}
