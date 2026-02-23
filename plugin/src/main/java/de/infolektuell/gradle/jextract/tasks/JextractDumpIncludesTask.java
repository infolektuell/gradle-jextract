package de.infolektuell.gradle.jextract.tasks;

import de.infolektuell.gradle.jextract.service.JextractStore;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

import java.nio.file.Path;

/// Uses Jextract to dump all included symbols of a library header into an arg file
@CacheableTask
public abstract class JextractDumpIncludesTask extends JextractBaseTask {
    /// Creates a new [JextractDumpIncludesTask] instance
    public JextractDumpIncludesTask() { super(); }

    /// The location of the generated arg file
    /// @return a property to configure the arg file location
    @OutputFile
    public abstract RegularFileProperty getArgFile();

    /// The task action that dumps all includes into an arg file
    @TaskAction
    protected final void dump() {
        JextractStore jextract = getJextractStore().get();
        switch (getInstallation().get()) {
            case RemoteJextractInstallation config -> jextract.exec(config.getJavaLanguageVersion().get(), this::commonExec);
            case LocalJextractInstallation config -> {
                Path installationPath = config.getLocation().getAsFile().get().toPath();
                jextract.exec(installationPath, this::commonExec);
            }
        }
    }

    private void commonExec(ExecSpec spec) {
        getIncludes().get().forEach(it -> spec.args("-I", it));
        spec.args("--dump-includes", getArgFile().get());
        spec.args(getHeader().get());
    }
}
