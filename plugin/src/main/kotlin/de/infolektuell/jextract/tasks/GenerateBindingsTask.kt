package de.infolektuell.jextract.tasks

import org.gradle.api.DefaultTask
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
    @get:OutputDirectory
    abstract val output: DirectoryProperty
    @get:InputFile
    abstract val header: RegularFileProperty
    @get:Optional
    @get:Input
    abstract val targetPackage: Property<String>
    @get:Optional
    @get:Input
    abstract val headerClassName: Property<String>

    @get:Optional
    @get:Input
    abstract val whitelist: MapProperty<String, ListProperty<String>>

    @get:Optional
    @get:Input
    abstract val useSystemLoadLibrary: Property<Boolean>
    @TaskAction
    fun execute() {
        execOperations.exec { action ->
            action.run {
                val tree = generator.get().asFileTree
                var cmd = "jextract"
                tree.visit { file ->
                    if (file.isDirectory) {
                        cmd = if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
                            file.file.resolve("bin/jextract.bat").absolutePath
                        } else {
                            file.file.resolve("bin/jextract").absolutePath
                        }
                        file.stopVisiting()
                    }
                }
                commandLine(cmd)
                args("--output", output.get())
                targetPackage.orNull?.let { args("-t", it) }
                headerClassName.orNull?.let { args("--header-class-name", it) }
                whitelist.orNull?.forEach { (k, v) ->
                    v.orNull?.forEach { args("--include-$k", it) }
                }

                useSystemLoadLibrary.orNull?.let { if (it) args("--use-system-load-library") }
                args(header.get())
            }
        }
    }
}
