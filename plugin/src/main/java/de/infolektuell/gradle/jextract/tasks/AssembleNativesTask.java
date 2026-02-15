package de.infolektuell.gradle.jextract.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

public abstract class AssembleNativesTask extends DefaultTask {
    @InputFiles
    public abstract ConfigurableFileCollection getFiles();
    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();
    @TaskAction
    protected void copy() {
        getFileSystemOperations().copy(spec -> {
            spec.from(getFiles());
            spec.into(getDestinationDirectory());
        });
    }
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();
}
