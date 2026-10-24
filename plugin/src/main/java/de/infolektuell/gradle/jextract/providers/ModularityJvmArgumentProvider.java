package de.infolektuell.gradle.jextract.providers;

import java.nio.file.Files;
import java.util.regex.Pattern;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

public abstract class ModularityJvmArgumentProvider implements CommandLineArgumentProvider {
    public static final Pattern modulePattern = Pattern.compile("^\\s*module\\s+([a-z.]+)");

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getSourceFiles();

    @Internal
    protected String getModuleName() {
        try {
            final var file = getSourceFiles().getSingleFile();
            final String text = Files.readString(file.toPath());
            final var matcher = modulePattern.matcher(text);
            final boolean found = matcher.find();
            return found ? matcher.group(1) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
