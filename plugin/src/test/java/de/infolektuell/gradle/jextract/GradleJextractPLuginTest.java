package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class GradleJextractPluginTest {
    @Test
    void registersExtension() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(GradleJextractPlugin.PLUGIN_NAME);
        Assertions.assertNotNull(project.getExtensions().findByType(JextractExtension.class));
    }
}
