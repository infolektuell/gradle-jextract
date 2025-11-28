package de.infolektuell.gradle.jextract.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

class PlatformTest {
    @Test
    void shouldDetectWindowsOS() {
        List<String> l = List.of("Windows", "Windows 11", "windows", "windows 10");
        l.forEach(it -> {
            var os = Platform.OperatingSystem.create(it);
            var platform = new Platform(os, Platform.Architecture.AARCH64);
            assertEquals(Platform.OperatingSystem.WINDOWS, os);
            assertTrue(platform.isWindows());
                assertFalse(platform.isMac());
            assertFalse(platform.isLinux());
        });
    }

    @Test
    void shouldDetectMacOs() {
        List.of("mac os x", "macOS", "MAC").forEach(it -> {
            var os = Platform.OperatingSystem.create(it);
            var platform = new Platform(os, Platform.Architecture.AARCH64);
            assertEquals(Platform.OperatingSystem.MAC, os);
            assertTrue(platform.isMac());
            assertFalse(platform.isLinux());
            assertFalse(platform.isWindows());
        });
    }

    @Test
    void shouldDetectLinuxOs() {
        List.of("Linux", "linux", "Unix").forEach(it -> {
            var os = Platform.OperatingSystem.create(it);
            var platform = new Platform(os, Platform.Architecture.AARCH64);
            assertEquals(Platform.OperatingSystem.LINUX, os);
            assertTrue(platform.isLinux());
                assertFalse(platform.isMac());
            assertFalse(platform.isWindows());
        });
    }
}
