package de.infolektuell.gradle.jextract.transforms;

import java.nio.file.Files;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.NonNull;

/// An artifact transform that wraps a file into a directory
@DisableCachingByDefault(because = "Copying files is not worth caching")
public abstract class DirectorifyAction implements TransformAction<TransformParameters.@NonNull None> {
    /// The artifact to be transformed
    @InputArtifact
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Provider<@NonNull FileSystemLocation> getInput();

    @Override
    public void transform(@NonNull TransformOutputs outputs) {
        final var input = getInput().get().getAsFile().toPath();
        if (Files.isDirectory(input)) outputs.dir(getInput());
        else {
            final var outDir = outputs.dir("libs").toPath();
            final var destFile = outDir.resolve(input.getFileName());
            try {
                Files.copy(input, destFile);
            } catch (Exception ignored) {}
        }
    }
}
