package de.infolektuell.gradle.jextract.providers;

import java.util.ArrayList;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.process.CommandLineArgumentProvider;

public abstract class EnableJvmPreviewProvider implements CommandLineArgumentProvider {
    @Input
    public abstract Property<JavaLanguageVersion> getJavaVersion();

    @Override
    public Iterable<String> asArguments() {
        final var args = new ArrayList<String>();
        if (getJavaVersion().get().asInt() < 22) {
            args.add("--enable-preview");
        }
        return args;
    }
}
