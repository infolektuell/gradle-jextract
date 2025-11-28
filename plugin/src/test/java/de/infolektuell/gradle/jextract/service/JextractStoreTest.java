package de.infolektuell.gradle.jextract.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JextractStoreTest {
    @Test
    void shouldParseVersionFromJextract25() {
        String output = """
                jextract 25
                JDK version 25+37-3491
                LibClang version clang version 13.0.0
            """;
        int version = JextractStore.parseExecutableVersion(output);
        assertEquals(25, version);
    }

    @Test
    void shouldParseVersionFromJextract22() {
        String output = """
                jextract 22
                JDK version 22+35-2369
                LibClang version clang version 13.0.0
            """;
        int version = JextractStore.parseExecutableVersion(output);
        assertEquals(22, version);
    }

    @Test
    void shouldParseVersionFromJextract21() {
        String output = """
                Jextract 21
                JDK version 21+35-LTS-2513
                LibClang version clang version 13.0.0
            """;
        int version = JextractStore.parseExecutableVersion(output);
        assertEquals(21, version);
    }

    @Test
    void shouldFailParsingFromMalformedString() {
        assertThrows(Exception.class, () -> JextractStore.parseExecutableVersion("\nJextract 22"));
    }

}
