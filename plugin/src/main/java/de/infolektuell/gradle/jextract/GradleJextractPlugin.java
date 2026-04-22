package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension;
import de.infolektuell.gradle.jextract.service.JextractStore;
import de.infolektuell.gradle.jextract.tasks.*;

import static de.infolektuell.gradle.jextract.tasks.JextractBaseTask.*;

import org.gradle.api.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

import java.util.*;

/// A Gradle plugin to add Jextract to the build
public abstract class GradleJextractPlugin implements Plugin<@NonNull Project> {
    /// The plugin ID that must be used in build scripts to apply the plugin
    public static final String PLUGIN_NAME = "de.infolektuell.jextract";

    ///  The gradle property to set a local Jextract installation path
    public static final String JEXTRACT_LOCAL_INSTALLATION_PROPERTY = "org.openjdk.jextract.installation-path";

    /// Used by Gradle
    public GradleJextractPlugin() { super(); }

    /// Configures the plugin if it is applied
    public void apply(Project project) {
        final JextractExtension extension = project.getObjects().newInstance(JextractExtension.class);
        project.getExtensions().add(JextractExtension.EXTENSION_NAME, extension);
        extension.getOutput().convention(project.getLayout().getBuildDirectory().dir("generated/sources/jextract/java"));
        extension.getGenerateSourceFiles().convention(false);
        extension.getInstallation().getDistributions().convention(extension.getDistributions());
        extension.getLibraries().configureEach(lib -> {
            lib.getLibraries().convention(List.of(lib.getName()));
            lib.getUseSystemLoadLibrary().convention(false);
            lib.getOutput().convention(extension.getOutput().dir(lib.getName()));
            lib.getGenerateSourceFiles().convention(extension.getGenerateSourceFiles());
        });

        project.getGradle().getSharedServices().registerIfAbsent(JextractStore.SERVICE_NAME, JextractStore.class, s -> {
            s.getMaxParallelUsages().convention(1);
            s.parameters(parameters -> {
                parameters.getCacheDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir(".gradle/jextract"));
                parameters.getDistributions().convention(extension.getInstallation().getDistributions());
            });
        });

        project.getPlugins().apply(JavaPlugin.class);
        project.getPluginManager().withPlugin("java", javaPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final Provider<@NonNull JavaLanguageVersion> javaVersion = javaExtension.getToolchain().getLanguageVersion()
                    .orElse(JavaLanguageVersion.of(Objects.requireNonNullElse(Jvm.current().getJavaVersionMajor(), 25)));
            extension.getInstallation().getJavaLanguageVersion().convention(javaVersion);

            final var localInstallationPath = project.getProviders().gradleProperty(JEXTRACT_LOCAL_INSTALLATION_PROPERTY);
            if (localInstallationPath.isPresent()) {
                final Directory location = project.getLayout().getProjectDirectory().dir(localInstallationPath.get());
                if (!location.getAsFile().isDirectory()) throw new GradleException(String.format("The path %s doesn't point to a directory.", location));
                extension.getInstallation().getLocation().convention(location);
            }
            final Provider<@NonNull JextractInstallation> jextractInstallation = extension.getInstallation().getLocation().map(location -> {
                    final var installation = project.getObjects().newInstance(LocalJextractInstallation.class);
                    installation.getLocation().convention(location);
                    return (JextractInstallation) installation;
                })
                .orElse(extension.getInstallation().getJavaLanguageVersion().map(version -> {
                    final var installation = project.getObjects().newInstance(RemoteJextractInstallation.class);
                    installation.getJavaLanguageVersion().convention(version);
                    return (JextractInstallation) installation;
                }));

            extension.getLibraries().configureEach(lib -> {
                project.getTasks().register(lib.getGenerateBindingsTaskName(), JextractGenerateTask.class, task -> {
                    task.setDescription("Uses Jextract to generate Java bindings for the " + lib.getName() + " native library");
                    task.getInstallation().convention(jextractInstallation);
                    task.getHeader().convention(lib.getHeader());
                    task.getIncludes().convention(lib.getIncludes());
                    task.getDefinedMacros().convention(lib.getDefinedMacros());
                    task.getHeaderClassName().convention(lib.getHeaderClassName());
                    task.getTargetPackage().convention(lib.getTargetPackage());
                    final Provider<@NonNull Map<@NonNull String, @NonNull Set<@NonNull String>>> whitelist = project.getProviders().provider(() -> {
                        return Map.of(
                            "function", lib.getWhitelist().getFunctions().get(),
                            "constant", lib.getWhitelist().getConstants().get(),
                            "struct", lib.getWhitelist().getStructs().get(),
                            "union", lib.getWhitelist().getUnions().get(),
                            "typedef", lib.getWhitelist().getTypedefs().get(),
                            "var", lib.getWhitelist().getVariables().get()
                        );
                    });
                    task.getWhitelist().convention(whitelist);
                    task.getArgFile().convention(lib.getWhitelist().getArgFile());
                    task.getLibraries().convention(lib.getLibraries());
                    task.getUseSystemLoadLibrary().convention(lib.getUseSystemLoadLibrary());
                    task.getGenerateSourceFiles().convention(lib.getGenerateSourceFiles());
                    task.getSources().convention(lib.getOutput());
                    task.getClasses().convention(project.getLayout().getBuildDirectory().dir("generated/classes/jextract/java/" + lib.getName()));
                });

                project.getTasks().register(lib.getDumpIncludesTaskName(), JextractDumpIncludesTask.class, task -> {
                    task.setDescription("Uses Jextract to dump all includes of the " + lib.getName() + " native library into an arg file");
                    task.getInstallation().convention(jextractInstallation);
                    task.getHeader().convention(lib.getHeader());
                    task.getIncludes().convention(lib.getIncludes());
                    task.getArgFile().convention(project.getLayout().getBuildDirectory().file("reports/jextract/" + lib.getName() + "-includes.txt"));
                });
            });

            javaExtension.getSourceSets().configureEach(s -> {
                final SourceSetExtension sourceSetExtension = project.getObjects().newInstance(SourceSetExtension.class);
                s.getExtensions().add(SourceSetExtension.EXTENSION_NAME, sourceSetExtension);
                sourceSetExtension.getLibraries().all(lib -> {
                    final TaskProvider<@NonNull JextractGenerateTask> task = project.getTasks().named(lib.getGenerateBindingsTaskName(), JextractGenerateTask.class);
                    s.getJava().srcDir(task.flatMap(JextractGenerateTask::getSources));
                    s.getResources().srcDir(task.flatMap(JextractGenerateTask::getSources));
                    final FileCollection classes = project.getLayout().files(task.flatMap(JextractGenerateTask::getClasses));
                    s.setCompileClasspath(s.getCompileClasspath().plus(classes));
                    s.setRuntimeClasspath(s.getRuntimeClasspath().plus(classes));
                });
            });
        });
    }
}
