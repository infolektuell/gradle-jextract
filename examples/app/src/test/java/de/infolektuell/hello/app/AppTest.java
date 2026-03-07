package de.infolektuell.hello.app;

import de.infolektuell.hello.bindings.Hello;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void ReturnsHelloWorld() {
        final var hello = Hello.hello_world().getString(0);
        Assertions.assertEquals("Hello World", hello);
    }
}
