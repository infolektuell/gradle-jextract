package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Provider;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;

/// An artifact transform that wraps a file into a directory
public abstract class DirectorifyAction implements TransformAction<TransformParameters.@NonNull None> {
    /// Used by gradle
    public DirectorifyAction() { super(); }

    /// The artifact to be transformed
    /// @return a provider to query the artifact
    @InputArtifact
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
