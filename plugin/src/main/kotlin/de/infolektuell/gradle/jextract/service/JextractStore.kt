package de.infolektuell.gradle.jextract.service

import de.infolektuell.gradle.jextract.model.DownloadClient
import de.infolektuell.gradle.jextract.model.JextractDataStore
import org.gradle.api.Action
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import javax.inject.Inject
import kotlin.io.path.absolute

abstract class JextractStore @Inject constructor(
    private val fileSystem: FileSystemOperations,
    private val archives: ArchiveOperations,
    private val execOperations: ExecOperations
) : BuildService<JextractStore.Parameters> {

    interface Parameters : BuildServiceParameters {
        val cacheDir: DirectoryProperty
        val distributions: RegularFileProperty
    }

    private val dataStore = JextractDataStore()
    private val downloadClient = DownloadClient()
    private val downloadsDir = parameters.cacheDir.dir("downloads")
    private val installDir = parameters.cacheDir.dir("installation")
    private val downloadsPaths = mutableMapOf<Int, Path>()
    private val installationsPaths = mutableMapOf<Int, Path>()
    private val executablePaths = mutableMapOf<Path, Path>()
    private val installationVersions = mutableMapOf<Path, Int?>()

    /** returns the Jextract version for a given [Java language version][JavaLanguageVersion] */
    fun version(javaLanguageVersion: JavaLanguageVersion) = dataStore.version(javaLanguageVersion.asInt())

    /**
     * Executes Jextract for a given [Java version][javaLanguageVersion] with a configurable [action]
     *
     * This is intended to be used by tasks.
     */
    fun exec (javaLanguageVersion: JavaLanguageVersion, action: Action<in ExecSpec>): ExecResult{
        val installation = install(javaLanguageVersion.asInt())
        return exec(installation, action)
    }

    /**
     * Executes Jextract installed in a custom [installation] path with a configurable [action]
     *
     * This is intended to be used by tasks.
     */
    fun exec (installation: Path, action: Action<in ExecSpec>): ExecResult {
        val executable = executablePaths.computeIfAbsent(installation) { k -> findExecutable(k) ?: throw RuntimeException("No valid Jextract installation") }
        return execOperations.exec { spec ->
            spec.executable(executable.absolute())
            action.execute(spec)
        }
    }

    /**
     * Registers a given [Jextract installation][installation] in the service and returns its version
     *
     * This is a utility method that can help tasks to adapt their command line arguments to the current Jextract version.
     */
    fun registerIfAbsent(installation: Path): Int? {
        return installationVersions.computeIfAbsent(installation) { k ->
            ByteArrayOutputStream().use { s ->
                exec(k) { spec ->
                    spec.args("--version")
                    spec.errorOutput = s
                }
                s.toString(Charset.defaultCharset())
                    .trim()
                    .lines()
                    .first()
                    .split(" ")
                    .last()
                    .toIntOrNull()
            }
        }
    }

    private fun download(javaLanguageVersion: Int): Path {
        val distributionsPath = parameters.distributions.orNull?.asFile?.toPath()?.takeIf { Files.exists(it) }
        val resource = dataStore.resource(javaLanguageVersion, distributionsPath)
        val target = downloadsPaths.computeIfAbsent(javaLanguageVersion) { k -> downloadsDir.get().asFile.toPath().resolve(dataStore.filename(k)) }
        if (!Files.exists(target)) {
            try {
                downloadClient.download(resource, target)
            } catch (e: RuntimeException) {
                downloadsPaths.remove(javaLanguageVersion)
                throw e
            }
        }
            return target
    }

    private fun install (javaLanguageVersion: Int): Path {
        val source = download(javaLanguageVersion)
        val target = installationsPaths.computeIfAbsent(javaLanguageVersion) { k -> installDir.get().asFile.toPath().resolve("$k") }
        if (!Files.exists(target)) {
            fileSystem.copy { spec ->
                spec.from(archives.tarTree(source.toFile()))
                spec.into(target)
            }
        }
        return target
    }

    fun findExecutable(path: Path): Path? {
        val glob = "glob:**/bin/${dataStore.executableFilename}"
        val pathMatcher: PathMatcher = FileSystems.getDefault().getPathMatcher(glob)
        return Files.walk(path)
            .filter(pathMatcher::matches)
            .filter { Files.isExecutable(it) && Files.isRegularFile(it) }
            .findFirst()
            .takeIf { it.isPresent }
            ?.get()
    }

    companion object {
        const val SERVICE_NAME = "jextractStore"
    }
}
