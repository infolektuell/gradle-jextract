package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Uses Jextract to dump all included symbols of a library header into an arg file */
@CacheableTask
abstract class JextractDumpIncludesTask : JextractBaseTask() {
    /** The location of the generated arg file */
    @get:OutputFile
    abstract val argFile: RegularFileProperty

    @TaskAction
    protected fun dump() {
        execute { spec ->
            includes.get().forEach { spec.args("-I", it.asFile.absolutePath) }
            spec.args("--dump-includes", argFile.get().asFile.absolutePath)
            spec.args(header.get().asFile.absolutePath)
        }
    }
}
