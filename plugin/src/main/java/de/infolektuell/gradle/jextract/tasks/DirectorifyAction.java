package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;

/// An artifact transform that wraps a file into a directory
@CacheableTransform
@DisableCachingByDefault(because = "Copying files not worth caching")
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
        if (getInput().get().getAsFile().isDirectory()) outputs.dir(getInput());
        else {
            final var dir = outputs.dir("libs");
            getFileSystemOperations().copy(spec -> {
                spec.from(getInput());
                spec.into(dir);
            });
        }
    }

    /// Inject the file system operations build service
    /// @return The injected build service
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();
}
