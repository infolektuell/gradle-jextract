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
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class JextractStore implements BuildService<JextractStore.@NonNull Parameters> {
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

    record Installation(Path root, Path executable, int version) {
    }

    record RemoteInstallation(DownloadClient.Resource resource, Path archive, Installation installation) {
    }

    private final JextractDataStore dataStore;
    private final DownloadClient downloadClient;
    private final Map<Integer, RemoteInstallation> remoteInstallations;
    private final Map<Path, Installation> localInstallations;

    public JextractStore() {
        super();
        this.dataStore = getParameters().getDistributions().isPresent() ? JextractDataStore.create(getParameters().getDistributions().get().getAsFile().toPath()) : JextractDataStore.create();
        this.downloadClient = new DownloadClient();
        this.remoteInstallations = new HashMap<>();
        this.localInstallations = new HashMap<>();
    }

    @Inject
    protected abstract FileSystemOperations getFileSystem();

    @Inject
    protected abstract ArchiveOperations getArchives();

    @Inject
    protected abstract ExecOperations getExecOperations();

    protected Provider<@NonNull Directory> getDownloadsDir() {
        return getParameters().getCacheDir().dir("downloads");
    }

    protected Provider<@NonNull Directory> getInstallDir() {
        return getParameters().getCacheDir().dir("installation");
    }

    /**
     * returns the Jextract version for a given [Java language version][JavaLanguageVersion]
     */
    public int getVersion(JavaLanguageVersion javaLanguageVersion) {
        return dataStore.version(javaLanguageVersion.asInt());
    }

    /**
     * Returns the version of a given [local installation][path], null if this is not installed
     */
    public int getVersion(Path path) {
        return install(path).version;
    }

    /**
     * Executes Jextract for a given [Java version][javaLanguageVersion] with a configurable [action]
     * <p>
     * This is intended to be used by tasks.
     */
    public ExecResult exec(JavaLanguageVersion version, Action<@NonNull ExecSpec> action) {
        return exec(version.asInt(), action);
    }

    /**
     * Executes Jextract installed in a custom [path][root] with a configurable [action]
     * <p>
     * This is intended to be used by tasks.
     */
    public ExecResult exec(Path root, Action<@NonNull ExecSpec> action) {
        return getExecOperations().exec(spec -> {
            spec.executable(install(root).executable.toAbsolutePath());
            action.execute(spec);
        });
    }

    private ExecResult exec(int version, Action<@NonNull ExecSpec> action) {
        return getExecOperations().exec(spec -> {
            spec.executable(install(version).installation.executable.toAbsolutePath());
            action.execute(spec);
        });
    }

    private Installation install(Path root) throws RuntimeException {
        return localInstallations.computeIfAbsent(root, k -> {
            try (var s = new ByteArrayOutputStream()) {
                var executable = findExecutable(k, dataStore.getExecutableFilename());
                getExecOperations().exec(spec -> {
                    spec.executable(executable);
                    spec.args("--version");
                    spec.setErrorOutput(s);
                });
                var output = s.toString(Charset.defaultCharset());
                var version = parseExecutableVersion(output);
                return new Installation(k, executable, version);
            } catch (Exception e) {
                throw new RuntimeException("Root does not contain a Jextract installation");
            }
        });
    }

    private RemoteInstallation install(int version) {
        return remoteInstallations.computeIfAbsent(version, k -> {
            final var resource = dataStore.resource(k);
            var archive = getDownloadsDir().get().file(dataStore.filename(k)).getAsFile().toPath();
            boolean isDownloaded = downloadClient.verify(resource, archive);
            if (!isDownloaded) downloadClient.download(resource, archive);
            var root = getInstallDir().get().dir(k.toString()).getAsFile().toPath();
            if (!isDownloaded) {
                getFileSystem().copy(spec -> {
                    spec.from(getArchives().tarTree(archive.toFile()));
                    spec.into(root);
                });
            }
            try {
                Path executable = findExecutable(root, dataStore.getExecutableFilename());
                var installation = new Installation(root, executable, version);
                return new RemoteInstallation(resource, archive, installation);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
