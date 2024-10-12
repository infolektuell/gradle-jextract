package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class DumpIncludesTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    interface LibraryConfig {
        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val header: RegularFileProperty
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val includes: ListProperty<Directory>
        @get:OutputFile
        val argFile: RegularFileProperty
    }

    protected abstract class DumpIncludesAction @Inject constructor(private val execOperations: ExecOperations) : WorkAction<DumpIncludesAction.Parameters> {
        interface Parameters : WorkParameters {
            val executable: RegularFileProperty
            val library: Property<LibraryConfig>
        }
        override fun execute() {
            execOperations.exec { spec ->
                spec.executable(parameters.executable.get().asFile.absolutePath)
                parameters.library.get().run {
                    includes.get().forEach { spec.args("-I", it.asFile.absolutePath) }
                    spec.args("--dump-includes", argFile.get().asFile.absolutePath)
                    spec.args(header.get().asFile.absolutePath)
                }
            }
        }
    }

    @get:Nested
    abstract val generator: Generator
    @get:Nested
    abstract val libraries: SetProperty<LibraryConfig>

    @TaskAction
    protected fun dump() {
        val queue = workerExecutor.noIsolation()
        libraries.get().forEach { config ->
            queue.submit(DumpIncludesAction::class.java) { param ->
                param.executable.set(generator.executable)
                param.library.set(config)
            }
        }
    }
}
