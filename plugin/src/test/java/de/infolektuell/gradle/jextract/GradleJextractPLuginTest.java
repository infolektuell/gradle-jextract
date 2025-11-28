package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GradleJextractPluginTest {
    @Test
    void registersExtension() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(GradleJextractPlugin.PLUGIN_NAME);
        assertNotNull(project.getExtensions().findByType(JextractExtension.class));
    }
}
