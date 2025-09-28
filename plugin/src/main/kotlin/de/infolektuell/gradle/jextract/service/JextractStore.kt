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
import java.io.*
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

    data class Installation(val root: Path, val executable: Path, val version: Int)
    data class RemoteInstallation(val resource: DownloadClient.Resource, val archive: Path, val installation: Installation)

    private val dataStore = JextractDataStore()
    private val downloadClient = DownloadClient()
    private val downloadsDir = parameters.cacheDir.dir("downloads")
    private val installDir = parameters.cacheDir.dir("installation")
    private val remoteInstallations = mutableMapOf<Int, RemoteInstallation>()
    private val localInstallations = mutableMapOf<Path, Installation>()

    /** returns the Jextract version for a given [Java language version][JavaLanguageVersion] */
    fun version(javaLanguageVersion: JavaLanguageVersion) = dataStore.version(javaLanguageVersion.asInt())

    /** Returns the version of a given [local installation][path], null if this is not installed */
    fun version(path: Path) = localInstallations[path]?.version ?: install(path).version

    /**
     * Executes Jextract for a given [Java version][javaLanguageVersion] with a configurable [action]
     *
     * This is intended to be used by tasks.
     */
    fun exec (javaLanguageVersion: JavaLanguageVersion, action: Action<in ExecSpec>): ExecResult = exec(dataStore.version(javaLanguageVersion.asInt()), action)
    private fun exec (version: Int, action: Action<in ExecSpec>): ExecResult{
        val data = remoteInstallations[version] ?: install(version)
        return execOperations.exec { spec ->
            spec.executable(data.installation.executable.absolute())
            action.execute(spec)
        }
    }

    /**
     * Executes Jextract installed in a custom [path][root] with a configurable [action]
     *
     * This is intended to be used by tasks.
     */
    fun exec (root: Path, action: Action<in ExecSpec>): ExecResult {
        val data = localInstallations[root] ?: install(root)
        return execOperations.exec { spec ->
            spec.executable(data.executable.absolute())
            action.execute(spec)
        }
    }

    private fun install(root: Path): Installation {
        return localInstallations.computeIfAbsent(root) { k ->
            val executable = findExecutable(k)
            val version = ByteArrayOutputStream().use { s ->
                execOperations.exec { spec ->
                    spec.executable(executable)
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
            } ?: throw RuntimeException("Couldn't parse version from local Jextract installation")
            Installation(k, executable, version)
        }
    }

    private fun install(version: Int): RemoteInstallation {
        return remoteInstallations.computeIfAbsent(version) { k ->
            val distributionsPath = parameters.distributions.orNull?.asFile?.toPath()?.takeIf { Files.exists(it) }
            val resource = dataStore.resource(k, distributionsPath)
            val archive = downloadsDir.get().asFile.toPath().resolve(dataStore.filename(k))
            if (!Files.exists(archive)) {
                downloadClient.download(resource, archive)
            }
            val root = installDir.get().asFile.toPath().resolve("$k")
            if (!Files.exists(root)) {
                fileSystem.copy { spec ->
                    spec.from(archives.tarTree(archive.toFile()))
                    spec.into(root)
                }
            }
            val executable = findExecutable(root)
            val installation = Installation(root, executable, version)
            RemoteInstallation(resource, archive, installation)
        }
    }

    private fun uninstall(version: Int): Boolean {
        val data = remoteInstallations[version] ?: return false
        fileSystem.delete { spec ->
            spec.delete(data.installation.root)
            spec.delete(data.archive)
        }
        remoteInstallations.remove(version)
        return true
    }

    private fun clear() {
        remoteInstallations.keys.forEach { uninstall(it) }
    }

    private fun findExecutable(path: Path): Path {
        val glob = "glob:**/bin/${dataStore.executableFilename}"
        val pathMatcher: PathMatcher = FileSystems.getDefault().getPathMatcher(glob)
        return Files.walk(path)
            .filter(pathMatcher::matches)
            .filter { Files.isExecutable(it) && Files.isRegularFile(it) }
            .findFirst()
            .takeIf { it.isPresent }
            ?.get()
            ?: throw RuntimeException("Executable not found in $path")
    }

    companion object {
        const val SERVICE_NAME = "jextractStore"
    }
}
