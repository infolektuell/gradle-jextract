package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

/**
 * Configures the Jextract installation location.
 */
public abstract class JextractExtension {
    public static final String EXTENSION_NAME = "jextract";

    /**
     * Configuration of a Jextract installation to be used by the plugin
     */
    @Nested
    public abstract InstallationHandler getInstallation();

    /**
     * Configures the plugin to download Jextract for a given Java language version
     */
    public final void download(@NonNull final JavaLanguageVersion javaLanguageVersion) {
        getInstallation().getJavaLanguageVersion().set(javaLanguageVersion);
    }

    /**
     * Configures a local Jextract installation to be used by the plugin
     */
    public final void local(@NonNull final DirectoryProperty location) {
        getInstallation().getLocation().set(location);
    }

    /**
     * Configures a local Jextract installation to be used by the plugin
     */
    public final void local(@NonNull final Directory location) {
        getInstallation().getLocation().set(location);
    }

    /**
     * A [properties][java.util.Properties] file containing the remote locations where to download the Jextract distributions
     */
    public abstract RegularFileProperty getDistributions();
}
