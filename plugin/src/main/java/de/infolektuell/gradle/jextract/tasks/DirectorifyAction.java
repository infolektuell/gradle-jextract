package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/// An artifact transform that wraps a file into a directory
@DisableCachingByDefault(because = "Copying files is not worth caching")
public abstract class DirectorifyAction implements TransformAction<TransformParameters.@NonNull None> {
    /// Used by gradle
    public DirectorifyAction() { super(); }

    /// The artifact to be transformed
    /// @return a provider to query the artifact
    @InputArtifact
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Provider<@NonNull FileSystemLocation> getInput();

    @Override
    public void transform(@NonNull TransformOutputs outputs) {
        final File input = getInput().get().getAsFile();
        if (input.isDirectory()) outputs.dir(input);
        else {
            final var dir = outputs.dir("libs");
            System.out.printf("Copying %s into %s%n", input.getAbsolutePath(), dir.getAbsolutePath());
            getFileSystemOperations().copy(spec -> {
                spec.from(input);
                spec.into(dir);
            });
            try (var s = Files.list(dir.toPath())) {
                final var content = s.map(Path::toString).collect(Collectors.toSet());
                System.out.printf("Output directory %s contains these files:%n", dir.getAbsolutePath());
                System.out.println(String.join("\n", content));
            } catch (Exception ignored) {
                System.err.println("Couldn't list content of transform directory");
            }
        }
    }

    /// Inject the file system operations build service
    /// @return The injected build service
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();
}
