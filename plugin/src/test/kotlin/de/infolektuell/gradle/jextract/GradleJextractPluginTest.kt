package de.infolektuell.gradle.jextract

import de.infolektuell.gradle.jextract.extensions.JextractExtension
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'org.example.greeting' plugin.
 */
class GradleJextractPluginTest {
    @Test fun `plugin registers extension`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(GradleJextractPlugin.PLUGIN_NAME)
        assertNotNull(project.extensions.findByType(JextractExtension::class.java))
    }
}
