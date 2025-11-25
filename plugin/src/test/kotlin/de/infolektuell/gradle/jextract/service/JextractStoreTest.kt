package de.infolektuell.gradle.jextract.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class JextractStoreTest {
    @Test fun `Should parse version from Jextract 25`() {
        val output = """
            jextract 25
            JDK version 25+37-3491
            LibClang version clang version 13.0.0
        """.trimIndent()
        val version = JextractStore.parseExecutableVersion(output)
        assertEquals(25, version)
    }

    @Test fun `Should parse version from Jextract 22`() {
        val output = """
            jextract 22
            JDK version 22+35-2369
            LibClang version clang version 13.0.0
        """.trimIndent()
        val version = JextractStore.parseExecutableVersion(output)
        assertEquals(22, version)
    }

    @Test fun `Should parse version from Jextract 21`() {
        val output = """
            Jextract 21
            JDK version 21+35-LTS-2513
            LibClang version clang version 13.0.0
        """.trimIndent()
        val version = JextractStore.parseExecutableVersion(output)
        assertEquals(21, version)
    }

    @Test fun `Should fail parsing version from malformed string`() {
        assertThrows(Exception::class.java)  { JextractStore.parseExecutableVersion("\nJextract 22") }
    }
}
