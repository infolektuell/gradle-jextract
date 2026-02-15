package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
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
import java.util.regex.Pattern;

/**
 * Creates a new JMOD archive.
 */
@CacheableTask
public abstract class JmodCreateTask extends DefaultTask {
    /**
     * The toolchain where the JMOD executable is used from.
     * @return A property where the value can be configured.
     */
    @Nested
    public abstract Property<@NonNull JavaInstallationMetadata> getMetadata();

    /**
     * Application jar files|dir containing classes   to be included in the JMOD. This must be a module.
     * @return A file collection where the files can be added.
     */
    @Classpath
    @InputFiles
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * Compression to use when creating the JMOD archive. Ranges from 0 to 9.
     * @return  A property where the value can be configured.
     */
    @Optional
    @Input
    public abstract Property<@NonNull Integer> getCompress();

    /**
     * Exclude files matching the supplied comma-separated pattern list. See java.nio.file.FileSystem#getPathMatcher for possible pattern formats.
     * @return A list property where the patterns can be added.
     */
    @Optional
    @Input
    public abstract ListProperty<@NonNull String> getExcludes();

    /**
     * Directories containing header files to be included in the jMOD archive.
     * @return A file collection to add the header files.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getHeaderFiles();

    /**
     * Directories containing legal notice files to be included in the jMOD archive.
     * @return A file collection to add the legal notices directories.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getLegalNotices();

    /**
     * Directories containing native binaries to be included in the jMOD archive.
     * @return A file collection to add the binaries directories.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getLibs();

    /**
     * Directories containing man pages to be included in the jMOD archive.
     * @return A file collection to add the man page directories.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getManPages();

    /**
     * The main class, if the JMOD is executable.
     * @return A property to configure the fully qualified name of the main class.
     */
    @Optional
    @Input
    public abstract Property<@NonNull String> getMainClass();

    /**
     * The module version.
     * @return A property to configure the module version.
     */
    @Optional
    @Input
    public abstract Property<@NonNull String> getModuleVersion();

    /**
     * The target platform.
     * @return A property to configure the target platform.
     */
    @Optional
    @Input
    public abstract Property<@NonNull String> getTargetPlatform();

    /**
     * The path where the JMOD archive will be created.
     * @return A file property to configure the location of the JMOD archive.
     */
    @OutputFile
    public abstract RegularFileProperty getJmod();

    @TaskAction
    protected void create() {
        final var executable = getMetadata().get().getInstallationPath().getAsFileTree().matching(m -> m.include("**/bin/jmod", "**/bin/jmod.exe")).getSingleFile();
        final var archive = getJmod().get().getAsFile().toPath();
        try {
            Files.deleteIfExists(archive);
        } catch (Exception ignored) {
            throw new GradleException("Couldn't delete jmod file " + archive);
        }

        getExecOperations().exec(spec -> {
            spec.executable(executable);
            spec.args("create");
            final FileCollection classpath = getClasspath().filter(File::exists);
            if (!classpath.isEmpty()) spec.args("--class-path", classpath.getAsPath());
            if (getCompress().isPresent()) spec.args("--compress", String.format("zip-%d", getCompress().get()));
            final var excludes = String.join(",", getExcludes().get());
            if (!excludes.isEmpty()) spec.args("--exclude", excludes);
            final FileCollection headers = getHeaderFiles().filter(File::exists);
            if (!headers.isEmpty()) spec.args("--header-files", headers.getAsPath());
            final FileCollection legal = getLegalNotices().filter(File::exists);
            if (!legal.isEmpty()) spec.args("--legal-notices", legal.getAsPath());
            final FileCollection libs = getLibs().filter(File::exists);
            if (!libs.isEmpty()) spec.args("--libs", libs.getAsPath());
            if (getMainClass().isPresent()) spec.args("--main-class", getMainClass().get());
            if (!getManPages().isEmpty()) spec.args("--man-pages", getManPages().getAsPath());
            if (getModuleVersion().isPresent()) {
                final var pattern = Pattern.compile("^[0-9]+(\\.[0-9]+){0,2}$");
                final var matcher = pattern.matcher(getModuleVersion().get());
                if (matcher.matches()) spec.args("--module-version", getModuleVersion().get());
            }
            if (getTargetPlatform().isPresent()) spec.args("--target-platform", getTargetPlatform().get());
            spec.args(getJmod().get());
        });
    }

    @Inject
    protected abstract ExecOperations getExecOperations();
}
