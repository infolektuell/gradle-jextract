package de.infolektuell.gradle.jextract.providers;

import java.util.ArrayList;
import java.util.Objects;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.InputFiles;

public abstract class PatchModuleProvider extends ModularityJvmArgumentProvider {
    @InputFiles
    @CompileClasspath
    public abstract ConfigurableFileCollection getClasspath();

    @Override
    public Iterable<String> asArguments() {
        final var args = new ArrayList<String>();
        final String moduleName = getModuleName();
        if (Objects.isNull(moduleName)) return args;
        args.add("--patch-module");
        args.add(String.format("%s=%s", moduleName, getClasspath().getAsPath()));
        return args;
    }
}
