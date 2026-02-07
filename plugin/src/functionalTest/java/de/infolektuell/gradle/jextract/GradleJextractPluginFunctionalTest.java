package de.infolektuell.gradle.jextract;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleJextractPluginFunctionalTest {
    private final Path projectDir = Paths.get("..", "examples");

    @Test
    void canRun() {
        var runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withProjectDir(projectDir.toFile());
        runner.withArguments("build");
        var result = runner.build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }
}
