package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;

public abstract class InstallationHandler {
    /** The [Java version][javaLanguageVersion] the code should be generated for */
    @NotNull
    public abstract Property<@NotNull JavaLanguageVersion> getJavaLanguageVersion();

    /** A directory containing a JExtract installation */
    @NotNull
    public abstract DirectoryProperty getLocation();
}
