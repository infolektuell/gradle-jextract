package de.infolektuell.hello.app;

import de.infolektuell.hello.bindings.Hello;
public class Main {
    public static void main(String[] arg) {
        final var hello = Hello.hello_world().getString(0);
        System.out.println(hello);
    }
}
