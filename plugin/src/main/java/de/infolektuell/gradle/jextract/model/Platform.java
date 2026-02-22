package de.infolektuell.gradle.jextract.model;

/// Describes a platform where jextract can be installed and run.
/// @param operatingSystem Indicates the platform's operating system
/// @param architecture    Indicates the platform's processor architecture
public record Platform(OperatingSystem operatingSystem, Architecture architecture) {

    /// Describes an operating system for a given platform.
    public enum OperatingSystem {
        /// The MS Windows operating system
        WINDOWS,
        /// The Apple macOS operating system
        MAC,
        /// The Linux operating system
        LINUX;

        /// Tries to find the matching operating system for a given string.
        /// @param value The string to match, e.g., from system properties.
        /// @return The matching architecture, LINUX if no match was found.
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

    /// Describes a processor architecture for a given platform.
    public enum Architecture {
        /// The AArch64 processor architecture
        AARCH64,
        /// The X64 processor architecture
        X64;

        /// Tries to find the matching architecture for a given string.
        /// @param value The string to match, e.g., from system properties.
        /// @return The matching architecture, X64 if no match was found.
        public static Architecture create(String value) {
            if (value.toLowerCase().contains("aarch64")) {
                return AARCH64;
            } else {
                return X64;
            }
        }
    }

    /// Checks if the platform's OS is Linux.
    /// @return True if the platform's OS is Linux.
    public boolean isLinux() {
        return operatingSystem == OperatingSystem.LINUX;
    }

    /// Checks if the platform's OS is macOS.
    /// @return True if the platform's OS is macOS.
    public boolean isMac() {
        return operatingSystem == OperatingSystem.MAC;
    }

    /// Checks if the platform's OS is MS Windows.
    /// @return True if the platform's OS is MS Windows.
    public boolean isWindows() {
        return operatingSystem == OperatingSystem.WINDOWS;
    }

    /// Checks if the platform's architecture is AArch64.
    /// @return True if the platform's architecture is AArch64.
    public boolean isArch64() {
        return architecture == Architecture.AARCH64;
    }

    /// Checks if the platform's architecture is X64.
    /// @return True if the platform's architecture is X64.
    public boolean isX64() {
        return architecture == Architecture.X64;
    }

    /// Tries to infer the current running platform.
    /// @return The inferred platform, Linux on X64 by default.
    public static Platform getCurrentPlatform() {
        OperatingSystem os = OperatingSystem.create(System.getProperty("os.name"));
        Architecture arch = Architecture.create(System.getProperty("os.arch"));
        return new Platform(os, arch);
    }
}
