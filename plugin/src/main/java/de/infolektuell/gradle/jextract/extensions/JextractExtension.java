package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;

///  The dSL extension to configure the jextract plugin
public abstract class JextractExtension {
    ///  The name that is used to register the extension
    public static final String EXTENSION_NAME = "jextract";
    private final NamedDomainObjectContainer<@NonNull LibraryHandler> libraries;

    ///  Creates a new [JextractExtension] instance
    public JextractExtension() {
        super();
        this.libraries = getObjects().domainObjectContainer(LibraryHandler.class);
    }

    /// Configuration of a Jextract installation to be used by the plugin
    @Nested
    public abstract InstallationHandler getInstallation();

    /// Configures the plugin to download Jextract for a given Java language version
    public final void download(@NonNull final JavaLanguageVersion javaLanguageVersion) {
        getInstallation().getJavaLanguageVersion().set(javaLanguageVersion);
    }

    /// Configures a local Jextract installation to be used by the plugin
    public final void local(@NonNull final DirectoryProperty location) {
        getInstallation().getLocation().set(location);
    }

    /// Configures a local Jextract installation to be used by the plugin
    public final void local(@NonNull final Directory location) {
        getInstallation().getLocation().set(location);
    }

    /// A [properties][java.util.Properties] file containing the remote locations where to download the Jextract distributions
    public abstract RegularFileProperty getDistributions();

    /// The libraries Jextract should generate bindings for
    public final NamedDomainObjectContainer<@NonNull LibraryHandler> getLibraries() {
        return this.libraries;
    }

    /// Configures the libraries Jextract should generate bindings for
    public final void libraries(@NonNull Action<@NonNull NamedDomainObjectContainer<@NonNull LibraryHandler>> action) {
        action.execute(this.libraries);
    }

    /// specify the directory to place generated files of all libraries by default
    public abstract DirectoryProperty getOutput();

    /// Generate source files instead of class files for all libraries where not set explicitly (Jextract 21 and below)
    public abstract Property<@NonNull Boolean> getGenerateSourceFiles();

    /// Inject an available instance of the object factory service
    /// @return The injected service instance
    @Inject
    protected abstract ObjectFactory getObjects();
}
