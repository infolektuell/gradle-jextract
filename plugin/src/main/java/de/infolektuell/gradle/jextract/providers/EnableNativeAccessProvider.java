package de.infolektuell.gradle.jextract.providers;

import java.util.List;
import java.util.Objects;

public abstract class EnableNativeAccessProvider extends ModularityJvmArgumentProvider {
    @Override
    public Iterable<String> asArguments() {
        final String moduleName = Objects.requireNonNullElse(getModuleName(), "ALL-UNNAMED");
        return List.of("--enable-native-access=" + moduleName);
    }
}
