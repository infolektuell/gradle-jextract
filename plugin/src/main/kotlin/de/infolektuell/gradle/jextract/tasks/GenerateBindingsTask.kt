package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

abstract class GenerateBindingsTask @Inject constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {
    @get:Nested
    abstract val generator: Generator
    @get:InputFile
    abstract val header: RegularFileProperty
    @get:Optional
    @get:Input
    abstract val targetPackage: Property<String>
    @get:Optional
    @get:Input
    abstract val headerClassName: Property<String>

    @get:InputFiles
    abstract val includes: ListProperty<Directory>
    @get:Input
    abstract val definedMacros: ListProperty<String>

    @get:Input
    abstract val whitelist: MapProperty<String, List<String>>
    @get:Optional
    @get:InputFile
    abstract val argFile: RegularFileProperty

    @get:Input
    abstract val libraries: ListProperty<String>
    @get:Input
    abstract val useSystemLoadLibrary: Property<Boolean>
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @TaskAction
    fun execute() {
        fileSystemOperations.delete { spec ->
            spec.delete(outputDirectory)
        }
        generator.execute { spec ->
            spec.args("--output", outputDirectory.get())
            targetPackage.orNull?.let { spec.args("-t", it) }
            headerClassName.orNull?.let { spec.args("--header-class-name", it) }
            includes.get().forEach { spec.args("-I", it.asFile.absolutePath) }
            definedMacros.get().forEach { spec.args("-D", it) }
            whitelist.get().forEach { (k, v) ->
                if (v.isEmpty()) return@forEach
                v.forEach { spec.args("--include-$k", it) }
            }
            libraries.get().forEach { spec.args("-l", it) }
            if (useSystemLoadLibrary.get()) spec.args("--use-system-load-library")
            argFile.orNull?.let { spec.args("@$it") }
            spec.args(header.get())
        }
    }
}
