package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

///  DSL extension to configure which Jextract installation should be used by the Jextract plugin
public abstract class InstallationHandler {
    /// Used by Gradle
    public InstallationHandler() { super(); }
    /// The Java version the code should be generated for
    /// @return A property to configure the Java version
    public abstract Property<@NonNull JavaLanguageVersion> getJavaLanguageVersion();

    /// The path of a local Jextract installation.
    /// If it is set, this installation is used by the plugin.
    /// @return A property to configure the JExtract installation path
    /// @deprecated Use Gradle property `org.openjdk.jextract.installaption-path` instead.
    public abstract DirectoryProperty getLocation();

    /// A [java.util.Properties] file containing the remote locations where to download the Jextract distributions
    /// @return a property
    public abstract RegularFileProperty getDistributions();
}
