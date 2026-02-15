package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension;
import de.infolektuell.gradle.jextract.service.JextractStore;
import de.infolektuell.gradle.jextract.tasks.*;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask.JextractInstallation;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask.LocalJextractInstallation;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask.RemoteJextractInstallation;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.*;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public abstract class GradleJextractPlugin implements Plugin<@NonNull Project> {
    public static final String PLUGIN_NAME = "de.infolektuell.jextract";

    public void apply(Project project) {
        project.getPluginManager().withPlugin("java", javaPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final JextractExtension extension = project.getExtensions().create(JextractExtension.EXTENSION_NAME, JextractExtension.class);
            extension.getInstallation().getJavaLanguageVersion().convention(JavaLanguageVersion.of(Objects.requireNonNullElse(Jvm.current().getJavaVersionMajor(), 25)));
            extension.getOutput().convention(project.getLayout().getBuildDirectory().dir("generated/sources/jextract"));
            extension.getGenerateSourceFiles().convention(false);
            extension.getInstallation().getJavaLanguageVersion().convention(javaExtension.getToolchain().getLanguageVersion());

            project.getGradle().getSharedServices().registerIfAbsent(JextractStore.SERVICE_NAME, JextractStore.class, s -> {
                s.getMaxParallelUsages().convention(1);
                s.parameters(p -> {
                    p.getCacheDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir(".gradle/jextract"));
                    p.getDistributions().convention(extension.getDistributions());
                });
            });

            project.getTasks().withType(JextractBaseTask.class, task -> {
                final var installation = extension.getInstallation().getLocation().map(location -> {
                    final LocalJextractInstallation local = project.getObjects().newInstance(LocalJextractInstallation.class);
                    local.getLocation().convention(location);
                    return (JextractInstallation)local;
                }).orElse(extension.getInstallation().getJavaLanguageVersion().map(javaLanguageVersion -> {
                    final RemoteJextractInstallation remote = project.getObjects().newInstance(RemoteJextractInstallation.class);
                    remote.getJavaLanguageVersion().convention(javaLanguageVersion);
                    return (JextractInstallation)remote;
                }));
                task.getInstallation().set(installation);
            });

            final Map<String, TaskProvider<@NonNull JextractGenerateTask>> jextractGenerateTasks = new HashMap<>();
            extension.getLibraries().configureEach(lib -> {
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
                final SourceSetExtension sourceSetExtension = project.getObjects().newInstance(SourceSetExtension.class, s.getName());
                s.getExtensions().add(SourceSetExtension.EXTENSION_NAME, sourceSetExtension);
                sourceSetExtension.getHeaders().srcDir(project.getLayout().getProjectDirectory().dir(String.format("src/%s/public", s.getName())));
                sourceSetExtension.getHeaders().getDestinationDirectory().convention(project.getLayout().getBuildDirectory().dir(String.format("native/%s/headers", s.getName())));
                sourceSetExtension.getBinaries().srcDir(project.getLayout().getProjectDirectory().dir(String.format("src/%s/lib", s.getName())));
                sourceSetExtension.getBinaries().getDestinationDirectory().convention(project.getLayout().getBuildDirectory().dir(String.format("native/%s/lib", s.getName())));
                sourceSetExtension.getLegalNotices().add(project.getLayout().getProjectDirectory().dir(String.format("src/%s/legal", s.getName())));
            });

            javaExtension.getSourceSets().named("main", s -> {
                final SourceSetExtension sourceSetExtension = s.getExtensions().getByType(SourceSetExtension.class);
                project.getConfigurations().dependencyScope(sourceSetExtension.getNativeImplementationConfigurationName());
                final NamedDomainObjectProvider<@NonNull ResolvableConfiguration> archiveHeaderFilesConfiguration = project.getConfigurations().resolvable(sourceSetExtension.getArchiveHeaderFilesConfigurationName(), config -> {
                    config.extendsFrom(project.getConfigurations().getByName(sourceSetExtension.getNativeImplementationConfigurationName()));
                    config.attributes(a -> {
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API));
                        a.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                    });
                });

                final NamedDomainObjectProvider<@NonNull ResolvableConfiguration> archiveBinaryConfiguration = project.getConfigurations().resolvable(sourceSetExtension.getArchiveBinaryConfigurationName(), config -> {
                    config.extendsFrom(project.getConfigurations().getByName(sourceSetExtension.getNativeImplementationConfigurationName()));
                    config.attributes(a -> {
                        a.attribute(Attribute.of("org.gradle.native.optimized", Boolean.class), true);
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME));
                    });
                });

                sourceSetExtension.getLibraries().configureEach(lib -> {
                    lib.getHeader().convention(sourceSetExtension.getHeaders().getClassesDirectory().map(d -> d.file(String.format("%s.h", lib.getName()))));
                    lib.getIncludes().add(sourceSetExtension.getHeaders().getClassesDirectory());
                    lib.getUseSystemLoadLibrary().convention(false);
                    lib.getLibraries().add(lib.getName());
                    lib.getOutput().convention(extension.getOutput().dir(lib.getName()));
                    lib.getGenerateSourceFiles().convention(extension.getGenerateSourceFiles());
                });

                final TaskProvider<@NonNull AssembleNativesTask> copyNativeHeadersTask = project.getTasks().register(s.getTaskName("copy", "nativeHeaders"), AssembleNativesTask.class, task -> {
                    task.getFiles().from(sourceSetExtension.getHeaders().getSourceDirectories(), archiveHeaderFilesConfiguration);
                    task.getDestinationDirectory().convention(sourceSetExtension.getHeaders().getDestinationDirectory());
                });
                sourceSetExtension.getHeaders().compiledBy(copyNativeHeadersTask, AssembleNativesTask::getDestinationDirectory);

                final TaskProvider<@NonNull AssembleNativesTask> copyNativeLibsTask = project.getTasks().register(s.getTaskName("copy", "nativeLibs"), AssembleNativesTask.class, task -> {
                    task.getFiles().from(sourceSetExtension.getBinaries().getSourceDirectories(), archiveBinaryConfiguration);
                    task.getDestinationDirectory().convention(sourceSetExtension.getBinaries().getDestinationDirectory());
                });
                sourceSetExtension.getBinaries().compiledBy(copyNativeLibsTask, AssembleNativesTask::getDestinationDirectory);
                sourceSetExtension.getLibraryPath().getFiles().from(sourceSetExtension.getBinaries().getClassesDirectory());

                final var isModularProject = s.getJava().getSourceDirectories().filter(this::isModule).getElements().map(e -> !e.isEmpty());
                final TaskProvider<@NonNull JmodCreateTask> createJmodTask = project.getTasks().register(s.getTaskName("create", "jmod"), JmodCreateTask.class, task -> {
                    task.setGroup("build");
                    task.setDescription("Converts a jar into a jmod file");
                    task.onlyIf(t -> isModularProject.get());
                    task.getMetadata().convention(getJavaToolchainService().launcherFor(javaExtension.getToolchain()).map(JavaLauncher::getMetadata));
                    task.getClasspath().from(s.getJava().getClassesDirectory(), project.getTasks().named(s.getProcessResourcesTaskName(), ProcessResources.class));
                    task.getHeaderFiles().from(sourceSetExtension.getHeaders().getClassesDirectory());
                    task.getLegalNotices().from(sourceSetExtension.getLegalNotices());
                    task.getLibs().from(sourceSetExtension.getBinaries().getClassesDirectory());
                    task.getJmod().convention(project.getLayout().getBuildDirectory().file(String.format("libs/%s.jmod", project.getName())));
                });

                project.getConfigurations().named(s.getApiElementsConfigurationName(), config -> {
                    config.getOutgoing().getVariants().register("jmod", jmod -> {
                        jmod.getDescription().set("A jmod file containing classes, resources, libs, headers, and legal notices if available.");
                        jmod.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                        jmod.artifact(createJmodTask.flatMap(JmodCreateTask::getJmod), artifact -> artifact.setType("jmod"));
                    });
                    config.getOutgoing().getVariants().register("headers", headers -> {
                        headers.getDescription().set("A directory that contains the assembled header files.");
                        headers.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.HEADERS_CPLUSPLUS));
                        headers.artifact(sourceSetExtension.getHeaders().getClassesDirectory(), artifact -> artifact.setType(ArtifactTypeDefinition.DIRECTORY_TYPE));
                    });
                });
                project.getConfigurations().named(s.getRuntimeElementsConfigurationName(), config -> {
                    config.getOutgoing().getVariants().register("jmod", jmod -> {
                        jmod.getDescription().set("A jmod file containing classes, resources, libs, headers, and legal notices if available.");
                        jmod.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                        jmod.artifact(createJmodTask.flatMap(JmodCreateTask::getJmod), artifact -> artifact.setType("jmod"));
                    });
                    config.getOutgoing().getVariants().register("libs", libs -> {
                        libs.getDescription().set("A directory that contains the assembled binaries.");
                        libs.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.DYNAMIC_LIB));
                        libs.artifact(sourceSetExtension.getBinaries().getClassesDirectory(), artifact -> artifact.setType(ArtifactTypeDefinition.DIRECTORY_TYPE));
                    });
                });

                sourceSetExtension.getLibraries().all(lib -> {
                    final Provider<@NonNull Directory> src = jextractGenerateTasks.get(lib.getName()).flatMap(JextractGenerateTask::getSources);
                    s.getJava().srcDir(src);
                    s.getResources().srcDir(src);
                    s.setCompileClasspath(s.getCompileClasspath().plus(project.getLayout().files(src)));
                    s.setRuntimeClasspath(s.getRuntimeClasspath().plus(project.getLayout().files(src)));
                });
            });

            javaExtension.getSourceSets().named("test", s -> {
                final SourceSetExtension sourceSetExtension = s.getExtensions().getByType(SourceSetExtension.class);
                final Provider<@NonNull ConfigurableFileCollection> mainLibs = javaExtension.getSourceSets().named("main").map(x -> x.getExtensions().getByType(SourceSetExtension.class).getLibraryPath().getFiles());
                sourceSetExtension.getLibraryPath().getFiles().from(mainLibs);
                project.getTasks().withType(Test.class, task -> task.getJvmArgumentProviders().add(sourceSetExtension.getLibraryPath()));
            });
        });

        project.getPluginManager().withPlugin("application", applicationPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final JavaApplication application = project.getExtensions().getByType(JavaApplication.class);
            javaExtension.getSourceSets().named("main", s -> {
                final SourceSetExtension sourceSetExtension = s.getExtensions().getByType(SourceSetExtension.class);
                project.getTasks().named(s.getTaskName("create", "jmod"), JmodCreateTask.class, task -> task.getMainClass().convention(application.getMainClass()));
                project.getTasks().named("run", JavaExec.class, task -> {
                    task.getJvmArgumentProviders().add(sourceSetExtension.getLibraryPath());
                });
            });
        });
    }

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
