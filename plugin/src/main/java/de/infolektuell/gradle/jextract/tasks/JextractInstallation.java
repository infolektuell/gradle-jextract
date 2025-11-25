package de.infolektuell.gradle.jextract.tasks;

/**
 * Configuration of a Jextract installation that can be used by this task
 */
public sealed interface JextractInstallation permits RemoteJextractInstallation, LocalJextractInstallation {
}
