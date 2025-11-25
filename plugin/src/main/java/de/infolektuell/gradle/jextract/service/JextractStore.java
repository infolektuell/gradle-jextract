package de.infolektuell.gradle.jextract.service;

import de.infolektuell.gradle.jextract.model.DownloadClient;
import de.infolektuell.gradle.jextract.model.JextractDataStore;
import org.gradle.api.Action;
import org.gradle.api.file.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;
import javax.inject.Inject;

public abstract class JextractStore implements BuildService<JextractStore.@NotNull Parameters> {
    public static final String SERVICE_NAME = "jextractStore";
    private static final Pattern versionPattern = Pattern.compile("jextract (?<version>\\d+)\\n+", Pattern.CASE_INSENSITIVE);
    static int parseExecutableVersion(String str) {
        var matcher = versionPattern.matcher(str);
        if (!matcher.find()) throw new RuntimeException("Couldn't parse version from local Jextract installation");
        return Integer.parseInt(matcher.group("version"));
    }
    static Path findExecutable(Path root, String filename) throws IOException {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/bin/" + filename);
        try (var s = Files.walk(root)) {
            var executable = s.filter(it -> pathMatcher.matches(it) && Files.isExecutable(it) && Files.isRegularFile(it))
                .findFirst();
            if (executable.isEmpty()) throw new RuntimeException("Executable not found");
            return executable.get();
        }
    }

    public interface Parameters extends BuildServiceParameters {
        DirectoryProperty getCacheDir();
        RegularFileProperty getDistributions();
    }
    record Installation(Path root, Path executable, int version) {}
    record RemoteInstallation(DownloadClient.Resource resource, Path archive, Installation installation) {}

    private final FileSystemOperations fileSystem;
    private final ArchiveOperations archives;
    private final ExecOperations execOperations;
    private final JextractDataStore dataStore;
    private final DownloadClient downloadClient;
    Map<Integer, RemoteInstallation> remoteInstallations;
    Map<Path, Installation> localInstallations;

    @Inject
    JextractStore(
        FileSystemOperations fileSystem,
        ArchiveOperations archives,
        ExecOperations execOperations
    ) {
        super();
        this.fileSystem = fileSystem;
        this.archives = archives;
        this.execOperations = execOperations;
        this.dataStore = new JextractDataStore();
        this.downloadClient = new DownloadClient();
        this.remoteInstallations = new HashMap<>();
        this.localInstallations = new HashMap<>();
    }

    private Provider<@NotNull Directory> getDownloadsDir() {
        return getParameters().getCacheDir().dir("downloads");
    }

    private Provider<@NotNull Directory> getInstallDir() {
        return getParameters().getCacheDir().dir("installation");
    }

    /** returns the Jextract version for a given [Java language version][JavaLanguageVersion] */
    public int getVersion(JavaLanguageVersion javaLanguageVersion) {
        return dataStore.version(javaLanguageVersion.asInt());
    }

    /** Returns the version of a given [local installation][path], null if this is not installed */
    public int getVersion(Path path) {
        return install(path).version;
    }

    /**
     * Executes Jextract for a given [Java version][javaLanguageVersion] with a configurable [action]
     * <p>
     * This is intended to be used by tasks.
     */
    public ExecResult exec(JavaLanguageVersion version, Action<@NotNull ExecSpec> action) {
        return exec(version.asInt(), action);
    }

    /**
     * Executes Jextract installed in a custom [path][root] with a configurable [action]
     * <p>
     * This is intended to be used by tasks.
     */
    public ExecResult exec(Path root, Action<@NotNull ExecSpec> action) {
        return execOperations.exec(spec -> {
            spec.executable(install(root).executable.toAbsolutePath());
            action.execute(spec);
        });
    }

    private ExecResult exec(int version, Action<@NotNull ExecSpec> action) {
        return execOperations.exec(spec -> {
            spec.executable(install(version).installation.executable.toAbsolutePath());
            action.execute(spec);
        });
    }

    private Installation install(Path root) throws RuntimeException {
        return localInstallations.computeIfAbsent(root, k -> {
            try (var s = new ByteArrayOutputStream()) {
                var executable = findExecutable(k, dataStore.getExecutableFilename());
                execOperations.exec(spec -> {
                    spec.executable(executable);
                    spec.args("--version");
                    spec.setErrorOutput(s);
                });
                    var output = s.toString(Charset.defaultCharset());
                var version = parseExecutableVersion(output);
                return new Installation(k, executable, version);
            } catch(Exception e) {
                throw new RuntimeException("Root does not contain a Jextract installation");
            }
        });
    }
    private RemoteInstallation install(int version) {
        return remoteInstallations.computeIfAbsent(version, k -> {
            DownloadClient.Resource resource;
            if (getParameters().getDistributions().isPresent()) {
                Path distributionsPath = getParameters().getDistributions().get().getAsFile().toPath();
                resource = Files.exists(distributionsPath) ? dataStore.resource(k, distributionsPath) : dataStore.resource(k, null);
            } else {
                resource = dataStore.resource(k, null);
            }
            var archive = getDownloadsDir().get().getAsFile().toPath().resolve(dataStore.filename(k, null));
            if (!Files.exists(archive)) {
                downloadClient.download(resource, archive);
            }
            var root = getInstallDir().get().getAsFile().toPath().resolve(k.toString());
            if (!Files.exists(root)) {
                fileSystem.copy(spec -> {
                    spec.from(archives.tarTree(archive.toFile()));
                    spec.into(root);
                });
            }
            Path executable;
            try {
                executable = findExecutable(root, dataStore.getExecutableFilename());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            var installation = new Installation(root, executable, version);
            return new RemoteInstallation(resource, archive, installation);
        });
    }
    private Boolean uninstall(int version) {
        if (!remoteInstallations.containsKey(version)) return false;
        var data = remoteInstallations.get(version);
        fileSystem.delete(spec -> {
            spec.delete(data.installation.root);
            spec.delete(data.archive);
        });
        remoteInstallations.remove(version);
        return true;
    }
    private void clean() {
        remoteInstallations.forEach((k, v) -> uninstall(k));
    }

}
