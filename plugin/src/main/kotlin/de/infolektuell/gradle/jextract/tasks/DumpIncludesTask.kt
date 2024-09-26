package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

abstract class DumpIncludesTask : DefaultTask() {
    @get:Nested
    abstract val generator: Generator
    @get:InputFile
    abstract val header: RegularFileProperty
    @get:OutputFile
    @get:Option(option = "arg-file", description = "The file to dump includes to")
    abstract val argFile: RegularFileProperty
    @TaskAction
    fun dump() {
        generator.execute { spec ->
            spec.args("--dump-includes", argFile.get().asFile.absolutePath)
            spec.args(header.get().asFile.absolutePath)
        }
    }
}
