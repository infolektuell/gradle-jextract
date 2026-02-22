package de.infolektuell.gradle.jextract.tasks;

import de.infolektuell.gradle.jextract.service.JextractStore;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.process.ExecSpec;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Set;

/// Task that uses Jextract to generate Java bindings for given C headers
@CacheableTask
public abstract class JextractGenerateTask extends JextractBaseTask {
    /// Creates a new [JextractGenerateTask] instance
    public JextractGenerateTask() { super(); }
    /// All macros defined for this library, conforming to the `name=value` pattern or `name` where `value` will be 1
    /// @return A list property to add defined macros
    @Input
    public abstract ListProperty<@NonNull String> getDefinedMacros();

    /// The package name for the generated classes (`unnamed` if missing).
    /// @return A property to configure the package name
    @Optional
    @Input
    public abstract Property<@NonNull String> getTargetPackage();

    /// Name of the generated header class (derived from header file name if missing)
    /// @return a property to configure the header name
    @Optional
    @Input
    public abstract Property<@NonNull String> getHeaderClassName();

    /// All symbols to be included in the generated bindings, grouped by their category
    /// The key of each entry denotes a category, the value is a list of symbols.
    /// @return A map property to add pairs of category and symbols
    @Input
    public abstract MapProperty<@NonNull String, @NonNull Set<String>> getWhitelist();

    /// An optional arg file for includes filtering
    /// @return a property to configure the file
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getArgFile();

    /// Native libraries to be loaded by generated code (either names or paths starting with colon)
    /// @return A property to add library names
    @Input
    public abstract ListProperty<@NonNull String> getLibraries();

    /// Load libraries in the library symbol lookup, using either `System.load` or `System.loadLibrary`, useful for libraries in java.library.path
    /// @return A boolean property
    @Optional
    @Input
    public abstract Property<@NonNull Boolean> getUseSystemLoadLibrary();

    /// Generate source files instead of class files (Jextract 21 and below)
    /// @return a boolean property
    @Optional
    @Input
    public abstract Property<@NonNull Boolean> getGenerateSourceFiles();

    /// The directory where to place the generated source files
    /// @return a directory property
    @OutputDirectory
    public abstract DirectoryProperty getSources();

    /// Task action that uses Jextract to generate Java bindings
    @TaskAction
    protected final void generateBindings() {
        JextractStore jextract = getJextractStore().get();
        switch (getInstallation().get()) {
            case RemoteJextractInstallation config -> {
                final JavaLanguageVersion javaVersion = config.getJavaLanguageVersion().get();
                final int version = jextract.getVersion(javaVersion);
                jextract.exec(javaVersion, spec -> commonExec(version, spec));
            }
            case LocalJextractInstallation config -> {
                Path installationPath = config.getLocation().getAsFile().get().toPath();
                final int version = jextract.getVersion(installationPath);
                jextract.exec(installationPath, spec -> commonExec(version, spec));
            }
        }
    }

    private void commonExec(int version, ExecSpec spec) {
        getIncludes().get().forEach(it -> spec.args("-I", it.getAsFile().getAbsolutePath()));
        spec.args("--output", getSources().get().getAsFile().getAbsolutePath());
        if (getTargetPackage().isPresent()) {
            spec.args("-t", getTargetPackage().get());
        }
        if (getHeaderClassName().isPresent()) {
            spec.args("--header-class-name", getHeaderClassName().get());
        }
        getDefinedMacros().get().forEach(it -> spec.args("-D", it));
        getWhitelist().get().forEach((k, v) -> v.forEach(it -> spec.args("--include-" + k, it)));
        getLibraries().get().forEach(it -> spec.args("-l", it));
        switch (version) {
            case 19, 20, 21 -> {
                if (getGenerateSourceFiles().get()) spec.args("--source");
            }
            default -> {
                if (getUseSystemLoadLibrary().get()) spec.args("--use-system-load-library");
            }
        }
        if (getArgFile().isPresent()) {
            spec.args("@" + getArgFile().get());
        }
        spec.args(getHeader().get());
    }
}
