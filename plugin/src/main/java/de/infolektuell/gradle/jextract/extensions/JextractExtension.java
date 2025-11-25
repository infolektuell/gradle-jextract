package de.infolektuell.gradle.jextract.extensions;

import de.infolektuell.gradle.jextract.tasks.JextractInstallation;
import de.infolektuell.gradle.jextract.tasks.LocalJextractInstallation;
import de.infolektuell.gradle.jextract.tasks.RemoteJextractInstallation;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public abstract class JextractExtension {
    @NotNull
    private final ObjectFactory objects;
    @NotNull
    private final NamedDomainObjectContainer<@NotNull LibraryHandler> libraries;
    @NotNull
    public static final String EXTENSION_NAME = "jextract";

    @Inject
    public JextractExtension(@NotNull ObjectFactory objects) {
        super();
        this.objects = objects;
        this.libraries = this.objects.domainObjectContainer(LibraryHandler.class);
    }

    /**
     * Configuration of a Jextract installation to be used by the plugin
     */
    @NotNull
    public abstract Property<@NotNull JextractInstallation> getInstallation();

    /**
     * Configures the Jextract installation to be downloaded from a remote location
     */
    public final void download(@NotNull Action<@NotNull RemoteJextractInstallation> action) {
        this.getInstallation().set(this.objects.newInstance(RemoteJextractInstallation.class, action));
    }

    /**
     * Configures the plugin to download Jextract for a given Java language version
     */
    public final void download(@NotNull final JavaLanguageVersion javaLanguageVersion) {
        this.download(it -> it.getJavaLanguageVersion().convention(javaLanguageVersion));
    }

    /**
     * Configures a local Jextract installation to be used by the plugin
     */
    public final void local(@NotNull Action<@NotNull LocalJextractInstallation> action) {
        this.getInstallation().set(objects.newInstance(LocalJextractInstallation.class, action));
    }

    /**
     * Configures a local Jextract installation to be used by the plugin
     */
    public final void local(@NotNull final DirectoryProperty location) {
        this.local(it -> it.getLocation().convention(location));
    }

    /**
     * Configures a local Jextract installation to be used by the plugin
     */
    public final void local(@NotNull final Directory location) {
        this.local(it -> it.getLocation().convention(location));
    }

    /**
     * A [properties][java.util.Properties] file containing the remote locations where to download the Jextract distributions
     */
    @NotNull
    public abstract RegularFileProperty getDistributions();

    /**
     * The libraries Jextract should generate bindings for
     */
    @NotNull
    public final NamedDomainObjectContainer<@NotNull LibraryHandler> getLibraries() {
        return this.libraries;
    }

    /**
     * Configures the libraries Jextract should generate bindings for
     */
    public final void libraries(@NotNull Action<@NotNull NamedDomainObjectContainer<@NotNull LibraryHandler>> action) {
        action.execute(this.libraries);
    }

    /**
     * specify the directory to place generated files of all libraries by default
     */
    @NotNull
    public abstract DirectoryProperty getOutput();

    /**
     * Generate source files instead of class files for all libraries where not set explicitly (Jextract 21 and below)
     */
    @NotNull
    public abstract Property<@NotNull Boolean> getGenerateSourceFiles();
}
