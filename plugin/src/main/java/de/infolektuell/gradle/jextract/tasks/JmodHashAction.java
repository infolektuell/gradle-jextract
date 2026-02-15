package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.process.ExecOperations;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;

/**
 * Records hashes of tied modules.
 */
public abstract class JmodHashAction implements Action<@NonNull Task> {
    /**
     * The toolchain where the jmod executable is used from.
     * @return A property where the value can be configured.
     */
    @Nested
    public abstract Property<@NonNull JavaInstallationMetadata> getMetadata();

    /**
     * Dry run of hash mode.
     * @return A property where the dry-run mode can be configured.
     */
    @Optional
    @Input
    public abstract Property<@NonNull Boolean> getDryRun();

    /**
     * Compute and record hashes to tie a packaged module with modules matching the given regex-pattern and depending upon it directly or indirectly. hashes are recorded in a JMOD file being created, or modular JAR file on the module path.
     * @return A list property where the patterns can be added.
     */
    @Optional
    @Input
    public abstract ListProperty<@NonNull String> getHashModules();

    /**
     * Module path
     * @return A file collection to add the modular files.
     */
    @Classpath
    @InputFiles
    public abstract ConfigurableFileCollection getModulePath();

    /**
     * The path where the JMOD archive will be created.
     * @return A file property to configure the location of the JMOD archive.
     */
    @InputFile
    public abstract RegularFileProperty getJmod();

    @Override
    public void execute(Task task) {
        final var executable = getMetadata().get().getInstallationPath().getAsFileTree().matching(m -> m.include("**/bin/jmod", "**/bin/jmod.exe")).getSingleFile();
        final var archive = getJmod().get().getAsFile().toPath();
        try {
            Files.deleteIfExists(archive);
        } catch (Exception ignored) {
            throw new GradleException("Couldn't delete jmod file " + archive);
        }

        getExecOperations().exec(spec -> {
            spec.executable(executable);
            spec.args("hash");
            if (getDryRun().getOrElse(false)) spec.args("--dry-run");
            final var modulePath = getModulePath().filter(File::exists);
            if (!modulePath.isEmpty()) spec.args("--module-path", modulePath.getAsPath());
            spec.args(getJmod().get());
        });
    }

    @Inject
    protected abstract ExecOperations getExecOperations();
}
