package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class JextractDumpIncludesTask : JextractBaseTask() {
    @get:OutputFile
    abstract val argFile: RegularFileProperty

    @TaskAction
    protected fun dump() {
        execOperations.exec { spec ->
            spec.executable(executable.get().absolutePath)
            includes.get().forEach { spec.args("-I", it.asFile.absolutePath) }
            spec.args("--dump-includes", argFile.get().asFile.absolutePath)
            spec.args(header.get().asFile.absolutePath)
        }
    }
}
