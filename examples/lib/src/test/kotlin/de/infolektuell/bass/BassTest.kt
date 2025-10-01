package de.infolektuell.bass

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BassTest {
    @Test
    fun extractsBassVersion() {
        val bass = Bass()
        assertEquals("2.4.17.0", bass.version.toString())
    }
}
