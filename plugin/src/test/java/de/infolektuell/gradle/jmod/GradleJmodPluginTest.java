package de.infolektuell.gradle.jmod;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GradleJmodPluginTest {
    @Test
    void appliesPlugin() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(GradleJmodPlugin.PLUGIN_NAME);
        assertNotNull(project.getPlugins().findPlugin(GradleJmodPlugin.class));
    }
}
