package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

///  DSL extension to configure which Jextract installation should be used by the Jextract plugin
public abstract class InstallationHandler {
    /// Creates a new instance
    public InstallationHandler() { super(); }
    /// The Java version the code should be generated for
    /// @return A property to configure the Java version
    public abstract Property<@NonNull JavaLanguageVersion> getJavaLanguageVersion();

    /// The path of a local Jextract installation.
    /// If it is set, this installation is used by the plugin.
    /// @return A property to configure the JExtract installation path
    public abstract DirectoryProperty getLocation();
}
