package de.infolektuell.gradle.jextract.model

import kotlin.test.Test
import kotlin.test.assertEquals

class JextractDataStoreTest {
    @Test fun `Should detect Windows OS`() {
        assertEquals(JextractDataStore.OS.WINDOWS, JextractDataStore.OS.create("Windows"))
        assertEquals(JextractDataStore.OS.WINDOWS, JextractDataStore.OS.create("Windows 11"))
        assertEquals(JextractDataStore.OS.WINDOWS, JextractDataStore.OS.create("windows"))
        assertEquals(JextractDataStore.OS.WINDOWS, JextractDataStore.OS.create("windows 10"))
    }
    @Test fun `Should detect macOS`() {
        assertEquals(JextractDataStore.OS.MAC, JextractDataStore.OS.create("mac os x"))
        assertEquals(JextractDataStore.OS.MAC, JextractDataStore.OS.create("macOS"))
        assertEquals(JextractDataStore.OS.MAC, JextractDataStore.OS.create("MAC"))
    }

    @Test fun `Should detect Linux`() {
        assertEquals(JextractDataStore.OS.LINUX, JextractDataStore.OS.create("Linux"))
        assertEquals(JextractDataStore.OS.LINUX, JextractDataStore.OS.create("linux"))
        assertEquals(JextractDataStore.OS.LINUX, JextractDataStore.OS.create("Unix"))
    }
}
