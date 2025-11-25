package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.NotNull;

/** Configuration of a local Jextract installation */
public non-sealed interface LocalJextractInstallation extends JextractInstallation {
    /** A directory containing a JExtract installation */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @NotNull
    DirectoryProperty getLocation();
}
