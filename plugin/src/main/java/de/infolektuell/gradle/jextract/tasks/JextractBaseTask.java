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
import org.jspecify.annotations.NonNull;

/// Holds common properties for Jextract-related tasks, does nothing itself
public abstract class JextractBaseTask extends DefaultTask {
    /// Configuration of a Jextract installation that can be used by this task
    public sealed interface JextractInstallation permits RemoteJextractInstallation, LocalJextractInstallation {}

    /// Configuration of a downloadable Jextract version
    public non-sealed interface RemoteJextractInstallation extends JextractInstallation {
        /// The Java version the code should be generated for
        /// @return A property to configure the Java version
        @Input
        Property<@NonNull JavaLanguageVersion> getJavaLanguageVersion();
    }

    /// Configuration of a local Jextract installation
    public non-sealed interface LocalJextractInstallation extends JextractInstallation {
        /// A directory containing a JExtract installation
        /// @return A property to configure the Jextract installation location
        @InputDirectory
        @PathSensitive(PathSensitivity.RELATIVE)
        DirectoryProperty getLocation();
    }

    /// Creates a new [JextractBaseTask] instance
    public JextractBaseTask() { super(); }

    /// A build service to run Jextract commands
    /// @return A property that holds the injected build service
    @ServiceReference("jextractStore")
    protected abstract Property<@NonNull JextractStore> getJextractStore();

    /// Configures which Jextract installation should be used
    /// @return A property that holds the Jextract installation data
    @Nested
    public abstract Property<@NonNull JextractInstallation> getInstallation();

    /// All directories to append to the list of include search paths
    /// @return A property to configure the include search path
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ListProperty<@NonNull Directory> getIncludes();

    /// The library header file to generate bindings for
    /// @return A property to configure the header file
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getHeader();
}
