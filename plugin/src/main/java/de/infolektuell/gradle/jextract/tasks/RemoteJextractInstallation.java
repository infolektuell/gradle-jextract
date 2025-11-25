package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;

/** Configuration of a downloadable Jextract version */
public non-sealed interface RemoteJextractInstallation extends JextractInstallation {
    /** The [Java version][javaLanguageVersion] the code should be generated for */
    @Input
    @NotNull
    Property<@NotNull JavaLanguageVersion> getJavaLanguageVersion();
}
