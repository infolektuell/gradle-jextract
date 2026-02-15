package de.infolektuell.bass;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BassTest {
    @Test
    void extractsBassVersion() {
        Bass bass = new Bass();
        Assertions.assertEquals("2.4.17.0", bass.getVersion().toString());
    }
}
