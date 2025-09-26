package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/** Extracts an [archive file][source] into a [directory][target] after deleting the target's content */
@DisableCachingByDefault(because = "Extracting an archive is not worth caching")
abstract class ExtractTask @Inject constructor(private val fileSystem: FileSystemOperations, private val archives: ArchiveOperations) : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val target: DirectoryProperty

    @TaskAction
    protected fun extract() {
        fileSystem.delete { spec ->
            spec.delete(target)
        }
        fileSystem.copy { spec ->
            spec.from(archives.tarTree(source))
            spec.into(target)
        }
    }
}
