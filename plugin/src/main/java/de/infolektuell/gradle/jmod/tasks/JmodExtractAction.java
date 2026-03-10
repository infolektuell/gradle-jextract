package de.infolektuell.gradle.jmod.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Extracts subdirectories from resolved JMOD files
@CacheableTransform
@DisableCachingByDefault(because = "Extracting archive not worth caching")
public abstract class JmodExtractAction implements TransformAction<JmodExtractAction.@NonNull Parameters> {
    /// The possible subdirectories that can be extracted
    public enum Prefix {
        /// The classes subdirectory that contains compiled Java classes
        CLASSES,
        /// The include subdirectory that contains native header files
        INCLUDE,
        /// The include subdirectory that contains native binary files
        LIB;

        @Override
        public String toString() { return this.name().toLowerCase(); }
    }

    ///  Configurable parameters for this artifact transform
    public interface Parameters extends TransformParameters {
        /// Indicates which subdirectory should be extracted
        /// @return A property to configure the prefix
        @Input
        Property<@NonNull Prefix> getPrefix();
    }

    /// Used by Gradle
    public JmodExtractAction() { super(); }

    /// The resolved artifact to be transformed
    /// @return A provider to query the artifact file location
    @InputArtifact
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Provider<@NonNull FileSystemLocation> getInputArtifact();

    @Override
    public void transform(@NonNull TransformOutputs outputs) {
        final File inFile = getInputArtifact().get().getAsFile();
        final Prefix prefix = getParameters().getPrefix().get();
        if (!inFile.exists()) return;
        switch (prefix) {
            case CLASSES -> {
                if (inFile.getName().endsWith(".jar")) {
                    outputs.file(inFile);
                }
                else if (inFile.getName().endsWith(".jmod")) {
                    extractSubdirectory(inFile, outputs.dir(getInputArtifact().get().getAsFile().getName().replaceAll("\\.jmod$", "")).toPath(), prefix.toString());
                }
            }
            case LIB, INCLUDE -> {
                if (inFile.getName().endsWith(".jmod")) {
                    extractSubdirectory(inFile, outputs.dir(getInputArtifact().get().getAsFile().getName().replaceAll("\\.jmod$", "")).toPath(), prefix.toString());
                }
            }
        }
    }

    private void extractSubdirectory(File archive, Path outDir, String subDir) {
        final Path prefixPath = Path.of(subDir);
        try (ZipFile zipFile = new ZipFile(archive)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry e = entries.nextElement();
                final Path namePath = Path.of(e.getName());
                if (!namePath.startsWith(prefixPath)) continue;
                final Path outPath = outDir.resolve(namePath.subpath(1, namePath.getNameCount()));
                Files.createDirectories(outPath.getParent());
                final var s = zipFile.getInputStream(e);
                Files.copy(s, outPath);
            }
        } catch (Exception ignored) {
            throw new GradleException(String.format("Failed to extract files from %s", archive.getName()));
        }
    }
}
