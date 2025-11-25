package de.infolektuell.gradle.jextract.tasks;

import de.infolektuell.gradle.jextract.service.JextractStore;

import java.nio.file.Path;
import java.util.Set;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.process.ExecSpec;
import org.jetbrains.annotations.NotNull;

@CacheableTask
public abstract class JextractGenerateTask extends JextractBaseTask {
    /** All macros defined for this library, conforming to the `name=value` pattern or `name` where `value` will be 1 */
    @Input
    @NotNull
    public abstract ListProperty<@NotNull String> getDefinedMacros();

    /** The package name for the generated classes (`unnamed` if missing). */
    @Optional
    @Input
    @NotNull
    public abstract Property<@NotNull String> getTargetPackage();

    /** Name of the generated header class (derived from header file name if missing) */
    @Optional
    @Input
    @NotNull
    public abstract Property<@NotNull String> getHeaderClassName();

    /** All symbols to be included in the generated bindings, grouped by their category */
    @Input
    @NotNull
    public abstract MapProperty<@NotNull String, @NotNull Set<String>> getWhitelist();

    /** An optional arg file for includes filtering */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @NotNull
    public abstract RegularFileProperty getArgFile();

    /** The names of the libraries that should be loaded by the generated header class */
    @Input
    @NotNull
    public abstract ListProperty<@NotNull String> getLibraries();

    /** If true, this instructs Jextract to load the shared libraries in the loader symbol lookup */
    @Optional
    @Input
    @NotNull
    public abstract Property<@NotNull Boolean> getUseSystemLoadLibrary();

    /** If true, this instructs Jextract 21 and older to generate source files instead of class files */
    @Optional
    @Input
    @NotNull
    public abstract Property<@NotNull Boolean> getGenerateSourceFiles();

    /** The directory where to place the generated source files */
    @OutputDirectory
    @NotNull
    public abstract DirectoryProperty getSources();

    @TaskAction
    protected final void generateBindings() {
        JextractStore jextract = this.getJextractStore().get();
        JextractInstallation config = this.getInstallation().get();
        if (config instanceof RemoteJextractInstallation) {
            final JavaLanguageVersion javaVersion = ((RemoteJextractInstallation) config).getJavaLanguageVersion().get();
            final int version = jextract.version(javaVersion);
            jextract.exec(javaVersion, spec -> commonExec(version, spec));
        } else if (config instanceof LocalJextractInstallation) {
            Path installationPath = ((LocalJextractInstallation) config).getLocation().getAsFile().get().toPath();
            final int version = jextract.version(installationPath);
            jextract.exec(installationPath, spec -> commonExec(version, spec));
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
