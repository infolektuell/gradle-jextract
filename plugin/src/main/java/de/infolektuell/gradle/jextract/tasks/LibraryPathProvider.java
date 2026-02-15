package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class LibraryPathProvider implements CommandLineArgumentProvider {
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
