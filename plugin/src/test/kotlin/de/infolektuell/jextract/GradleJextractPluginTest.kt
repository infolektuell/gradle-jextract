package de.infolektuell.jextract

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'org.example.greeting' plugin.
 */
class GradleJextractPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("de.infolektuell.jextract")

        // Verify the result
        assertNotNull(project.tasks.findByName("greeting"))
    }
}
