package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.*;

public abstract class InstallationHandler {
    /** The [Java version][javaLanguageVersion] the code should be generated for */
    public abstract Property<@NonNull JavaLanguageVersion> getJavaLanguageVersion();

    /** A directory containing a JExtract installation */
    public abstract DirectoryProperty getLocation();
}
