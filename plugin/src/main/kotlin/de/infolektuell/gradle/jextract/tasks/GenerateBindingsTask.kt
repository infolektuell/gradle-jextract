package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class GenerateBindingsTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    interface LibraryConfig {
        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val header: RegularFileProperty
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val includes: ListProperty<Directory>
        @get:Input
        val definedMacros: ListProperty<String>
        @get:Optional
        @get:Input
        val targetPackage: Property<String>
        @get:Optional
        @get:Input
        val headerClassName: Property<String>
        @get:Input
        val whitelist: MapProperty<String, Set<String>>
        @get:Optional
        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val argFile: RegularFileProperty
        @get:Input
        val libraries: ListProperty<String>
        @get:Optional
        @get:Input
        val useSystemLoadLibrary: Property<Boolean>
        @get:Optional
        @get:Input
        val generateSourceFiles: Property<Boolean>
        @get:OutputDirectory
        val sources: DirectoryProperty
    }

    protected abstract class GenerateBindingsAction @Inject constructor(private val fileSystemOperations: FileSystemOperations, private val execOperations: ExecOperations) : WorkAction<GenerateBindingsAction.Parameters> {
        interface Parameters : WorkParameters {
            val executable: Property<String>
            val version: Property<Int>
            val library: Property<LibraryConfig>
        }

        override fun execute() {
            val version = parameters.version.get()
            parameters.library.get().run {
            fileSystemOperations.delete { spec ->
                spec.delete(sources)
            }
            execOperations.exec { spec ->
                spec.executable(parameters.executable.get())
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
                    if (useSystemLoadLibrary.get() && version >= 22) spec.args("--use-system-load-library")
                if (version <= 21 && generateSourceFiles.get()) spec.args("--source")
                    argFile.orNull?.let { spec.args("@$it") }
                    spec.args(header.get())
                }
            }
        }
    }

    @get:Nested
    abstract val generator: Generator
    @get:Nested
    abstract val libraries: SetProperty<LibraryConfig>
    @TaskAction
    protected fun generateBindings() {
        val executable = generator.findExecutable()
        val queue = workerExecutor.noIsolation()
        libraries.get().forEach { lib ->
            queue.submit(GenerateBindingsAction::class.java) { param ->
                param.executable.set(executable)
                param.version.set(generator.version)
                param.library.set(lib)
            }
        }
    }
}
