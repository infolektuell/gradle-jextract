package de.infolektuell.gradle.jextract.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PlatformTest {
    @Test
    void shouldDetectWindowsOS() {
        List<String> l = List.of("Windows", "Windows 11", "windows", "windows 10");
        l.forEach(it -> {
            var os = Platform.OperatingSystem.create(it);
            var platform = new Platform(os, Platform.Architecture.AARCH64);
            Assertions.assertEquals(Platform.OperatingSystem.WINDOWS, os);
            Assertions.assertTrue(platform.isWindows());
                Assertions.assertFalse(platform.isMac());
            Assertions.assertFalse(platform.isLinux());
        });
    }

    @Test
    void shouldDetectMacOs() {
        List.of("mac os x", "macOS", "MAC").forEach(it -> {
            var os = Platform.OperatingSystem.create(it);
            var platform = new Platform(os, Platform.Architecture.AARCH64);
            Assertions.assertEquals(Platform.OperatingSystem.MAC, os);
            Assertions.assertTrue(platform.isMac());
            Assertions.assertFalse(platform.isLinux());
            Assertions.assertFalse(platform.isWindows());
        });
    }

    @Test
    void shouldDetectLinuxOs() {
        List.of("Linux", "linux", "Unix").forEach(it -> {
            var os = Platform.OperatingSystem.create(it);
            var platform = new Platform(os, Platform.Architecture.AARCH64);
            Assertions.assertEquals(Platform.OperatingSystem.LINUX, os);
            Assertions.assertTrue(platform.isLinux());
                Assertions.    assertFalse(platform.isMac());
            Assertions.assertFalse(platform.isWindows());
        });
    }
}
