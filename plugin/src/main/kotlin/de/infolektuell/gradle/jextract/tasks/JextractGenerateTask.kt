package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class JextractGenerateTask @Inject constructor(private val fileSystemOperations: FileSystemOperations) : JextractBaseTask() {
    @get:Input
    abstract val definedMacros: ListProperty<String>
    @get:Optional
    @get:Input
    abstract val targetPackage: Property<String>
    @get:Optional
    @get:Input
    abstract val headerClassName: Property<String>
    @get:Input
    abstract val whitelist: MapProperty<String, Set<String>>
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val argFile: RegularFileProperty
    @get:Input
    abstract val libraries: ListProperty<String>
    @get:Optional
    @get:Input
    abstract val useSystemLoadLibrary: Property<Boolean>
    @get:Optional
    @get:Input
    abstract val generateSourceFiles: Property<Boolean>
    @get:OutputDirectory
    abstract val sources: DirectoryProperty

    @TaskAction
    protected fun generateBindings() {
        val version = executableVersion()
            ?: throw GradleException("Couldn't recognize the version of the given Jextract distribution.")
        fileSystemOperations.delete { spec ->
            spec.delete(sources)
        }
        execOperations.exec { spec ->
            spec.executable(executable.get().absolutePath)
            spec.args("--output", sources.get().asFile.absolutePath)
            targetPackage.orNull?.let { spec.args("-t", it) }
            headerClassName.orNull?.let { spec.args("--header-class-name", it) }
            includes.get().forEach { spec.args("-I", it.asFile.absolutePath) }
            definedMacros.get().forEach { spec.args("-D", it) }
            whitelist.get().forEach { (k, v) ->
                if (v.isEmpty()) return@forEach
                v.forEach { spec.args("--include-$k", it) }
            }
            libraries.get().forEach { spec.args("-l", it) }
            when(version) {
                19, 20, 21 -> {
                    if (generateSourceFiles.get()) spec.args("--source")
                }
                22 -> {
                    if (useSystemLoadLibrary.get()) spec.args("--use-system-load-library")
                }
            }
            argFile.orNull?.let { spec.args("@$it") }
            spec.args(header.get())
        }
    }
}
