package de.infolektuell.gradle.jextract.tasks;

import de.infolektuell.gradle.jextract.service.JextractStore;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Uses Jextract to dump all included symbols of a library header into an arg file
 */
@CacheableTask
public abstract class JextractDumpIncludesTask extends JextractBaseTask {
    /**
     * The location of the generated arg file
     */
    @OutputFile
    @NotNull
    public abstract RegularFileProperty getArgFile();

    @TaskAction
    protected final void dump() {
        JextractStore jextract = this.getJextractStore().get();
        JextractInstallation config = this.getInstallation().get();
        if (config instanceof RemoteJextractInstallation) {
            jextract.exec(((RemoteJextractInstallation) config).getJavaLanguageVersion().get(), spec -> {
                this.getIncludes().get().forEach(it -> spec.args("-I", it.getAsFile().getAbsolutePath()));
                spec.args("--dump-includes", getArgFile().get().getAsFile().getAbsolutePath());
                spec.args(getHeader().get().getAsFile().getAbsolutePath());
            });
        } else if (config instanceof LocalJextractInstallation) {
            Path installationPath = ((LocalJextractInstallation) config).getLocation().getAsFile().get().toPath();
            jextract.exec(installationPath, spec -> {
                getIncludes().get().forEach(it -> spec.args("-I", it.getAsFile().getAbsolutePath()));
                spec.args("--dump-includes", getArgFile().get().getAsFile().getAbsolutePath());
                spec.args(getHeader().get().getAsFile().getAbsolutePath());
            });
        }
    }
}
