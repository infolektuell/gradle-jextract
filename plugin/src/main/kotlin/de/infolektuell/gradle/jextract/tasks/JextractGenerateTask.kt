package de.infolektuell.gradle.jextract.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecSpec

/** Uses Jextract to generate Java bindings for a given native library */
@CacheableTask
abstract class JextractGenerateTask : JextractBaseTask() {
    /** All macros defined for this library, conforming to the `<name>=<value>` pattern or `<name>` where `<value>` will be 1 */
    @get:Input
    abstract val definedMacros: ListProperty<String>

    /** The package name for the generated classes (`unnamed` if missing). */
    @get:Optional
    @get:Input
    abstract val targetPackage: Property<String>

    /** Name of the generated header class (derived from header file name if missing) */
    @get:Optional
    @get:Input
    abstract val headerClassName: Property<String>

    /** All symbols to be included in the generated bindings, grouped by their category */
    @get:Input
    abstract val whitelist: MapProperty<String, Set<String>>

    /** An optional arg file for includes filtering */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val argFile: RegularFileProperty

    /** The names of the libraries that should be loaded by the generated header class */
    @get:Input
    abstract val libraries: ListProperty<String>

    /** If true, this instructs Jextract to load the shared libraries in the loader symbol lookup */
    @get:Optional
    @get:Input
    abstract val useSystemLoadLibrary: Property<Boolean>

    /** If true, this instructs Jextract 21 and older to generate source files instead of class files */
    @get:Optional
    @get:Input
    abstract val generateSourceFiles: Property<Boolean>

    /** The directory where to place the generated source files */
    @get:OutputDirectory
    abstract val sources: DirectoryProperty

    @TaskAction
    protected fun generateBindings() {
        val jextract = jextractStore.get()
        when (val config = installation.get()) {
            is RemoteJextractInstallation -> {
                val version = jextract.version(config.javaLanguageVersion.get())
                jextract.exec(config.javaLanguageVersion.get(), config.distributions.orNull?.asFile?.toPath()) { spec ->
                    commonExec(version, spec)
                }
            }
            is LocalJextractInstallation -> {
                val installationPath = config.location.asFile.get().toPath()
                val version = jextract.registerIfAbsent(installationPath)
                    ?: throw GradleException("Couldn't recognize the version of the given Jextract distribution.")
                jextract.exec(installationPath) { spec ->
                    commonExec(version, spec)
                }
            }
        }
    }

    private fun commonExec(version: Int, spec: ExecSpec) {
        includes.get().forEach { spec.args("-I", it.asFile.absolutePath) }
        spec.args("--output", sources.get().asFile.absolutePath)
        targetPackage.orNull?.let { spec.args("-t", it) }
        headerClassName.orNull?.let { spec.args("--header-class-name", it) }
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
