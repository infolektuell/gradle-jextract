package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension;
import de.infolektuell.gradle.jextract.service.JextractStore;
import de.infolektuell.gradle.jextract.tasks.*;

import static de.infolektuell.gradle.jextract.tasks.JextractBaseTask.*;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaCompiler;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// A Gradle plugin to add Jextract to the build
@SuppressWarnings("UnstableApiUsage")
public abstract class GradleJextractPlugin implements Plugin<@NonNull Project> {
    /// The plugin ID that must be used in build scripts to apply the plugin
    public static final String PLUGIN_NAME = "de.infolektuell.jextract";

    ///  Creates a new instance
    public GradleJextractPlugin() { super(); }

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

            final LibraryPathProvider libraryPathProvider = project.getObjects().newInstance(LibraryPathProvider.class);
            project.getTasks().withType(Test.class, task -> {
                task.getJvmArgumentProviders().add(libraryPathProvider);
            });

            final Map<String, TaskProvider<@NonNull JextractGenerateTask>> jextractGenerateTasks = new HashMap<>();
            extension.getLibraries().configureEach(lib -> {
                lib.getUseSystemLoadLibrary().convention(false);
                lib.getOutput().convention(extension.getOutput().dir(lib.getName()));
                lib.getGenerateSourceFiles().convention(extension.getGenerateSourceFiles());
                lib.getIncludes().add(lib.getHeader().map(h -> project.getLayout().getProjectDirectory().dir(h.getAsFile().getParentFile().getAbsolutePath())));
                libraryPathProvider.getFiles().from(lib.getLibraryPath());

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
            javaExtension.getSourceSets().named("main", s -> {
                final var isModularProject = s.getJava().getSourceDirectories().filter(this::isModule).getElements().map(e -> !e.isEmpty());
                final TaskProvider<@NonNull JmodCreateTask> createJmodTask = project.getTasks().register(s.getTaskName("create", "Jmod"), JmodCreateTask.class, task -> {
                    task.setGroup("build");
                    task.setDescription("Converts a jar into a jmod file");
                    task.onlyIf(t -> isModularProject.get());
                    task.getMetadata().convention(javaVersion.flatMap(v -> getJavaToolchainService().compilerFor(spec -> spec.getLanguageVersion().set(v)).map(JavaCompiler::getMetadata)));
                    task.getClasspath().from(s.getOutput());
                    task.getJmod().convention(project.getLayout().getBuildDirectory().file(String.format("libs/%s.jmod", project.getName())));
                });

                project.getConfigurations().named(s.getApiElementsConfigurationName(), config -> {
                    config.getOutgoing().getVariants().register("jmod", jmod -> {
                        jmod.getDescription().set("A jmod file containing classes, resources, libs, headers, and legal notices if available.");
                        jmod.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                        jmod.artifact(createJmodTask, artifact -> artifact.setType("jmod"));
                    });
                });
                project.getConfigurations().named(s.getRuntimeElementsConfigurationName(), config -> {
                    config.getOutgoing().getVariants().register("jmod", jmod -> {
                        jmod.getDescription().set("A jmod file containing classes, resources, libs, headers, and legal notices if available.");
                        jmod.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                        jmod.artifact(createJmodTask, artifact -> artifact.setType("jmod"));
                    });
                });

                final SourceSetExtension sourceSetExtension = s.getExtensions().getByType(SourceSetExtension.class);
                sourceSetExtension.getLibraries().all(lib -> {
                    createJmodTask.configure(task -> {
                        task.getHeaderFiles().from(lib.getIncludes());
                        task.getLibs().from(lib.getLibraryPath());
                    });
                });
            });
        });

        project.getPluginManager().withPlugin("application", applicationPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final JavaApplication application = project.getExtensions().getByType(JavaApplication.class);
            javaExtension.getSourceSets().named("main", s -> {
                project.getTasks().named(s.getTaskName("create", "jmod"), JmodCreateTask.class, task -> {
                    task.getMainClass().convention(application.getMainClass());
                });
            });

            final LibraryPathProvider libraryPathProvider = project.getObjects().newInstance(LibraryPathProvider.class);
            extension.getLibraries().all(lib -> {
                libraryPathProvider.getFiles().from(lib.getLibraryPath());
            });
            project.getTasks().named("run", JavaExec.class, task -> {
                task.getJvmArgumentProviders().add(libraryPathProvider);
            });
        });
    }

    ///  Injects the build service for Java toolchains
    /// @return A property holding the injected build service
    @Inject
    protected abstract JavaToolchainService getJavaToolchainService();

    private boolean isModule(File file) {
        if (file.isFile() && file.getName().endsWith(".jmod")) return true;
        if (!file.isDirectory()) return false;
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/module-info.{class,java}");
        try (var stream = Files.walk(file.toPath(), 2)) {
            return stream.anyMatch(matcher::matches);
        } catch (Exception ignored) {
            return false;
        }
    }
}
