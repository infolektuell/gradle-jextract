package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

/// Configures the native library path for Java execution.
public abstract class LibraryPathProvider implements CommandLineArgumentProvider {
    /// Creates a new instance
    public LibraryPathProvider() { super(); }

    /// The directories containing native libraries that are necessary for Java runtime
    /// @return a file collection of directories
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getFiles();

    @Override
    public List<String> asArguments() {
        final List<String> args = new ArrayList<>();
        if (getFiles().isEmpty()) return args;
        args.add(String.join("=", "-Djava.library.path", getFiles().getAsPath()));
        return args;
    }
}
