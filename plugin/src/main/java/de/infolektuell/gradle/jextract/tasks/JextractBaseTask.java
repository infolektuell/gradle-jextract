package de.infolektuell.gradle.jextract.tasks;

import de.infolektuell.gradle.jextract.service.JextractStore;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.NotNull;

/** Holds common properties for Jextract-related tasks, does nothing itself */
public abstract class JextractBaseTask extends DefaultTask {
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
