package de.infolektuell.gradle.jextract.model

import de.infolektuell.gradle.jextract.model.Platform.Architecture.AARCH64
import de.infolektuell.gradle.jextract.model.Platform.Architecture.X64
import de.infolektuell.gradle.jextract.model.Platform.OperatingSystem.LINUX
import de.infolektuell.gradle.jextract.model.Platform.OperatingSystem.MAC
import de.infolektuell.gradle.jextract.model.Platform.OperatingSystem.WINDOWS

class Platform(val operatingSystem: OperatingSystem, val architecture: Architecture) {
    enum class OperatingSystem {
        WINDOWS, MAC, LINUX;
        companion object {
            fun create(value: String): OperatingSystem {
                return if (value.contains("Windows", true)) {
                    WINDOWS
                } else if (value.contains("Mac", true)) {
                    MAC
                } else {
                    LINUX
                }
            }
        }
    }

    enum class Architecture {
        AARCH64, X64;
        companion object {
            fun create(value: String): Architecture {
                return if (value.contains("aarch64", true)) {
                    AARCH64
                } else {
                    X64
                }
            }
        }
    }

    val isLinux: Boolean get() = operatingSystem == LINUX
    val isMac: Boolean get() = operatingSystem == MAC
    val isWindows: Boolean get() = operatingSystem == WINDOWS
    val isArch64: Boolean get() = architecture == AARCH64
    val isX64: Boolean get() = architecture == X64
    companion object {
        fun getCurrentPlatform(): Platform {
            val os = OperatingSystem.create(System.getProperty("os.name"))
            val arch = Architecture.create(System.getProperty("os.arch"))
            return Platform(os, arch)
        }
    }
}
