package de.infolektuell.gradle.jextract.tasks
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class ExtractTask @Inject constructor(private val fileSystem: FileSystemOperations, private val archives: ArchiveOperations) : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty
    @get:OutputDirectory
    abstract val target: DirectoryProperty
    @TaskAction
    fun run() {
        fileSystem.delete { spec ->
            spec.delete(target)
        }
        fileSystem.copy { spec ->
            spec.from(archives.tarTree(source))
            spec.into(target)
        }
    }
}
