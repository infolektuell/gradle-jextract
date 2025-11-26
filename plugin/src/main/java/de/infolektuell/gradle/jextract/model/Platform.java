package de.infolektuell.gradle.jextract.model;

public record Platform(OperatingSystem operatingSystem, Architecture architecture) {

    public enum OperatingSystem {
        WINDOWS, MAC, LINUX;

        public static OperatingSystem create(String value) {
            if (value.toLowerCase().contains("windows")) {
                return WINDOWS;
            } else if (value.toLowerCase().contains("mac")) {
                return MAC;
            } else {
                return LINUX;
            }
        }
    }

    public enum Architecture {
        AARCH64, X64;

        public static Architecture create(String value) {
            if (value.toLowerCase().contains("aarch64")) {
                return AARCH64;
            } else {
                return X64;
            }
        }
    }

    public boolean isLinux() {
        return operatingSystem == OperatingSystem.LINUX;
    }

    public boolean isMac() {
        return operatingSystem == OperatingSystem.MAC;
    }

    public boolean isWindows() {
        return operatingSystem == OperatingSystem.WINDOWS;
    }

    public boolean isArch64() {
        return architecture == Architecture.AARCH64;
    }

    public boolean isX64() {
        return architecture == Architecture.X64;
    }

    public static Platform getCurrentPlatform() {
        OperatingSystem os = OperatingSystem.create(System.getProperty("os.name"));
        Architecture arch = Architecture.create(System.getProperty("os.arch"));
        return new Platform(os, arch);
    }
}
