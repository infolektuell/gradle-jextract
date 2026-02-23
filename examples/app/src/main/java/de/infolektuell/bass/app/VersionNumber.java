package de.infolektuell.bass.app;

public record VersionNumber(int major, int minor, int patch, int build) {
    @Override
    public String toString() {
        return major + "." + minor + "." + patch + "." + build;
    }
}
