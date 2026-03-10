package de.infolektuell.hello;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HelloTest {
    @Test
    void extractsBassVersion() {
        final var hello = Hello.sayHello();
        Assertions.assertEquals("Hello World", hello);
    }
}
