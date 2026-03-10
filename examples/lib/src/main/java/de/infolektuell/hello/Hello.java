package de.infolektuell.hello;
import static de.infolektuell.hello.bindings.Hello.hello_world;

public class Hello {
    public static String sayHello() {
        final var hello = hello_world().getString(0);
        return hello;
    }
}
