package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

/// Configures the native library path for Java execution.
public abstract class LibraryPathProvider implements CommandLineArgumentProvider {
    /// If explicitly set to false, no command line arguments are returned
    @Input
    @Optional
    public abstract Property<Boolean> getIsEnabled();

    /// The directories containing native libraries that are necessary for Java runtime
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getDirectories();

    @Override
    public List<String> asArguments() {
        final List<String> args = new ArrayList<>();
        if (!getIsEnabled().getOrElse(true)) return args;
        if (getDirectories().isEmpty()) return args;
        args.add(String.join("=", "-Djava.library.path", getDirectories().getAsPath()));
        return args;
    }
}
