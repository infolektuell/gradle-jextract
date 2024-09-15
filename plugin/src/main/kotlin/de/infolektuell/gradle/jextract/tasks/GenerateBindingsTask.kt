package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class GenerateBindingsTask @Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    abstract val generator: DirectoryProperty
    @get:InputFile
    abstract val header: RegularFileProperty
    @get:Optional
    @get:Input
    abstract val targetPackage: Property<String>
    @get:Optional
    @get:Input
    abstract val headerClassName: Property<String>

    @get:Optional
    @get:InputFiles
    abstract val includes: ListProperty<Directory>

    @get:Optional
    @get:Input
    abstract val whitelist: MapProperty<String, List<String>>

    @get:Optional
    @get:Input
    abstract val libraries: ListProperty<String>
    @get:Optional
    @get:Input
    abstract val useSystemLoadLibrary: Property<Boolean>
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @TaskAction
    fun execute() {
        val cmdFile = generator.asFileTree.matching { spec ->
            val fileName = if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) "jextract.bat" else "jextract"
            spec.include("**/bin/$fileName")
        }.singleFile
        execOperations.exec { action ->
            action.run {
                commandLine(cmdFile.absolutePath)
                args("--output", outputDirectory.get())
                targetPackage.orNull?.let { args("-t", it) }
                headerClassName.orNull?.let { args("--header-class-name", it) }
                includes.orNull?.forEach { args("-I", it.asFile.absolutePath) }
                whitelist.orNull?.forEach { (k, v) ->
                    v.forEach { args("--include-$k", it) }
                }

                libraries.orNull?.forEach { args("-l", it) }
                useSystemLoadLibrary.orNull?.let { if (it) args("--use-system-load-library") }
                args(header.get())
            }
        }
    }
}
