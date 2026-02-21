package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension;
import de.infolektuell.gradle.jextract.service.JextractStore;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask;
import static de.infolektuell.gradle.jextract.tasks.JextractBaseTask.*;
import de.infolektuell.gradle.jextract.tasks.JextractDumpIncludesTask;
import de.infolektuell.gradle.jextract.tasks.JextractGenerateTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GradleJextractPlugin implements Plugin<@NonNull Project> {
    /// The plugin ID that must be used in build scripts to apply the plugin
    public static final String PLUGIN_NAME = "de.infolektuell.jextract";

    /// Configures the plugin if it is applied
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        final JextractExtension extension = project.getObjects().newInstance(JextractExtension.class);
        project.getExtensions().add(JextractExtension.EXTENSION_NAME, extension);
        project.getPluginManager().withPlugin("java", javaPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final Provider<@NonNull JavaLanguageVersion> javaVersion = javaExtension.getToolchain().getLanguageVersion()
                    .orElse(JavaLanguageVersion.of(Objects.requireNonNullElse(Jvm.current().getJavaVersionMajor(), 25)));
            extension.getInstallation().getJavaLanguageVersion().convention(javaVersion);
            extension.getOutput().convention(project.getLayout().getBuildDirectory().dir("generated/sources/jextract"));
            extension.getGenerateSourceFiles().convention(false);

            project.getGradle().getSharedServices().registerIfAbsent(JextractStore.SERVICE_NAME, JextractStore.class, s -> {
                s.getMaxParallelUsages().convention(1);
                s.parameters(parameters -> {
                    parameters.getCacheDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir(".gradle/jextract"));
                    parameters.getDistributions().convention(extension.getDistributions());
                });
            });

            project.getTasks().withType(JextractBaseTask.class, task -> {
                final var jextractInstallation = extension.getInstallation().getLocation().map(location -> {
                        final var installation = project.getObjects().newInstance(LocalJextractInstallation.class);
                        installation.getLocation().convention(location);
                        return (JextractInstallation) installation;
                    })
                    .orElse(extension.getInstallation().getJavaLanguageVersion().map(version -> {
                        final var installation = project.getObjects().newInstance(RemoteJextractInstallation.class);
                        installation.getJavaLanguageVersion().convention(version);
                        return (JextractInstallation) installation;
                    }));
                task.getInstallation().convention(jextractInstallation);
            });

            final Map<String, TaskProvider<@NonNull JextractGenerateTask>> jextractGenerateTasks = new HashMap<>();
            extension.getLibraries().configureEach(lib -> {
                lib.getUseSystemLoadLibrary().convention(false);
                lib.getOutput().convention(extension.getOutput().dir(lib.getName()));
                lib.getGenerateSourceFiles().convention(extension.getGenerateSourceFiles());

                final TaskProvider<@NonNull JextractGenerateTask> generateTaskProvider = project.getTasks().register(lib.getName() + "JextractGenerateBindings", JextractGenerateTask.class, task -> {
                    task.setDescription("Uses Jextract to generate Java bindings for the " + lib.getName() + " native library");
                    task.getHeader().set(lib.getHeader());
                    task.getIncludes().set(lib.getIncludes());
                    task.getDefinedMacros().set(lib.getDefinedMacros());
                    task.getHeaderClassName().set(lib.getHeaderClassName());
                    task.getTargetPackage().set(lib.getTargetPackage());
                    task.getWhitelist().put("function", lib.getWhitelist().getFunctions());
                    task.getWhitelist().put("constant", lib.getWhitelist().getConstants());
                    task.getWhitelist().put("struct", lib.getWhitelist().getStructs());
                    task.getWhitelist().put("union", lib.getWhitelist().getUnions());
                    task.getWhitelist().put("typedef", lib.getWhitelist().getTypedefs());
                    task.getWhitelist().put("var", lib.getWhitelist().getVariables());
                    task.getArgFile().set(lib.getWhitelist().getArgFile());
                    task.getLibraries().set(lib.getLibraries());
                    task.getUseSystemLoadLibrary().set(lib.getUseSystemLoadLibrary());
                    task.getGenerateSourceFiles().set(lib.getGenerateSourceFiles());
                    task.getSources().set(lib.getOutput());
                });
                jextractGenerateTasks.put(lib.getName(), generateTaskProvider);
                project.getTasks().register(lib.getName() + "JextractDumpIncludes", JextractDumpIncludesTask.class, task -> {
                    task.setDescription("Uses Jextract to dump all includes of the " + lib.getName() + " native library into an arg file");
                    task.getHeader().set(lib.getHeader());
                    task.getIncludes().set(lib.getIncludes());
                    task.getArgFile().set(project.getLayout().getBuildDirectory().file("reports/jextract/" + lib.getName() + "-includes.txt"));
                });
            });

            javaExtension.getSourceSets().configureEach(s -> {
                final SourceSetExtension sourceSetExtension = project.getObjects().newInstance(SourceSetExtension.class);
                s.getExtensions().add(SourceSetExtension.EXTENSION_NAME, sourceSetExtension);
                sourceSetExtension.getLibraries().all(lib -> {
                    final TaskProvider<@NonNull JextractGenerateTask> task = jextractGenerateTasks.get(lib.getName());
                    s.getJava().srcDir(task);
                    s.getResources().srcDir(task);
                    final FileCollection src = project.getLayout().files(task);
                    s.setCompileClasspath(s.getCompileClasspath().plus(src));
                    s.setRuntimeClasspath(s.getRuntimeClasspath().plus(src));
                });
            });
        });
    }
}
