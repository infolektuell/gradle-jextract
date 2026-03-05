package de.infolektuell.gradle.jextract;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleJextractPluginFunctionalTest {

    private Path getProjectDir() { return Path.of("..", "examples"); }

    @Test
    void canBuild() {
        var runner = GradleRunner.create();
        runner.withProjectDir(getProjectDir().toFile());
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("lib:clean", "lib:build", "--stacktrace");
        var result = runner.build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void canRun() {
        var runner = GradleRunner.create();
        runner.withProjectDir(getProjectDir().toFile());
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("app:clean", "app:run", "--stacktrace");
        var result = runner.build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }
}
