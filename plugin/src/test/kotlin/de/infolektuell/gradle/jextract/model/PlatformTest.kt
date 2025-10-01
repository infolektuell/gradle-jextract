package de.infolektuell.gradle.jextract.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PlatformTest {
    @Test fun `Should detect Windows OS`() {
        listOf("Windows", "Windows 11", "windows", "windows 10")
        .forEach {
            val os = Platform.OperatingSystem.create(it)
            val platform = Platform(os, Platform.Architecture.AARCH64)
            assertEquals(Platform.OperatingSystem.WINDOWS, os)
            assert(platform.isWindows)
            assertFalse(platform.isMac)
            assertFalse(platform.isLinux)
        }
    }
    @Test fun `Should detect macOS`() {
        listOf("mac os x", "macOS", "MAC")
            .forEach {
                val os = Platform.OperatingSystem.create(it)
                val platform = Platform(os, Platform.Architecture.AARCH64)
                assertEquals(Platform.OperatingSystem.MAC, os)
                assert(platform.isMac)
                assertFalse(platform.isLinux)
                assertFalse(platform.isWindows)
            }
    }

    @Test fun `Should detect Linux`() {
        listOf("Linux", "linux", "Unix")
            .forEach {
                val os = Platform.OperatingSystem.create(it)
                val platform = Platform(os, Platform.Architecture.AARCH64)
                assertEquals(Platform.OperatingSystem.LINUX, os)
                assert(platform.isLinux)
                assertFalse(platform.isMac)
                assertFalse(platform.isWindows)
            }
    }
}
