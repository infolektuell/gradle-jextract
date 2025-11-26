package de.infolektuell.gradle.jextract;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class GradleJextractPluginFunctionalTest {
    @TempDir
    Path projectDir;

    private Path getBuildFile() {
        return projectDir.resolve("build.gradle");
    }

    private Path getSettingsFile() {
        return projectDir.resolve("settings.gradle");
    }

    @Test
    void canRun() throws IOException {
        String buildScript = """
            plugins {
                id('de.infolektuell.jextract') version '1.0'
            }
            """;
        Files.writeString(getSettingsFile(), "");
        Files.writeString(getBuildFile(), buildScript);
        var runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        // runner.withArguments("greeting")
        runner.withProjectDir(projectDir.toFile());
        var result = runner.build();
    }
}
