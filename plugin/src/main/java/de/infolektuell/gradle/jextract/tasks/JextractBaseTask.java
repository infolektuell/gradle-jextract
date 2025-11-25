package de.infolektuell.gradle.jextract.tasks;

import de.infolektuell.gradle.jextract.service.JextractStore;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;

/** Holds common properties for Jextract-related tasks, does nothing itself */
public abstract class JextractBaseTask extends DefaultTask {
    /**
     * Configuration of a Jextract installation that can be used by this task
     */
    public sealed interface JextractInstallation permits RemoteJextractInstallation, LocalJextractInstallation {}

    /** Configuration of a downloadable Jextract version */
    public non-sealed interface RemoteJextractInstallation extends JextractInstallation {
        /** The [Java version][javaLanguageVersion] the code should be generated for */
        @Input
        @NotNull
        Property<@NotNull JavaLanguageVersion> getJavaLanguageVersion();
    }

    /** Configuration of a local Jextract installation */
    public non-sealed interface LocalJextractInstallation extends JextractInstallation {
        /** A directory containing a JExtract installation */
        @InputDirectory
        @PathSensitive(PathSensitivity.RELATIVE)
        @NotNull
        DirectoryProperty getLocation();
    }

    /** A build service to run Jextract commands */
    @ServiceReference("jextractStore")
    @NotNull
    protected abstract Property<@NotNull JextractStore> getJextractStore();

    /** Configures which Jextract installation should be used */
    @Nested
    @NotNull
    public abstract Property<@NotNull JextractInstallation> getInstallation();

    /** All directories to append to the list of include search paths */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @NotNull
    public abstract ListProperty<@NotNull Directory> getIncludes();

    /** The library header file to generate bindings for */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @NotNull
    public abstract RegularFileProperty getHeader();
}
