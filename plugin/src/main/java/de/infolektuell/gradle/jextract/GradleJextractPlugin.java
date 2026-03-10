package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension;
import de.infolektuell.gradle.jextract.service.JextractStore;
import de.infolektuell.gradle.jextract.tasks.*;

import static de.infolektuell.gradle.jextract.tasks.JextractBaseTask.*;

import de.infolektuell.gradle.jmod.GradleJmodPlugin;
import de.infolektuell.gradle.jmod.extensions.JmodSourceSetExtension;
import org.gradle.api.*;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A Gradle plugin to add Jextract to the build
@SuppressWarnings("UnstableApiUsage")
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
            lib.getDependencies().getHeaderFilter().convention(Set.of("**/" + lib.getName() + ".h"));
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

        project.getDependencies().registerTransform(DirectorifyAction.class, transform -> {
            transform.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "dll");
            transform.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
        });
        project.getDependencies().registerTransform(DirectorifyAction.class, transform -> {
            transform.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "dylib");
            transform.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
        });
        project.getDependencies().registerTransform(DirectorifyAction.class, transform -> {
            transform.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "so");
            transform.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
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
                final NamedDomainObjectProvider<@NonNull DependencyScopeConfiguration> headerOnlyScope = project.getConfigurations().dependencyScope(lib.getName() + "JextractHeaderOnly", config -> config.fromDependencyCollector(lib.getDependencies().getHeaderOnly()));
                final NamedDomainObjectProvider<@NonNull DependencyScopeConfiguration> headerScope = project.getConfigurations().dependencyScope(lib.getName() + "JextractHeader", config -> config.fromDependencyCollector(lib.getDependencies().getHeader()));
                final NamedDomainObjectProvider<@NonNull DependencyScopeConfiguration> includeOnlyScope = project.getConfigurations().dependencyScope(lib.getName() + "JextractIncludeOnly", config -> config.fromDependencyCollector(lib.getDependencies().getIncludeOnly()));
                final NamedDomainObjectProvider<@NonNull DependencyScopeConfiguration> includeScope = project.getConfigurations().dependencyScope(lib.getName() + "JextractInclude", config -> config.fromDependencyCollector(lib.getDependencies().getInclude()));
                final NamedDomainObjectProvider<@NonNull DependencyScopeConfiguration> runtimeOnlyScope = project.getConfigurations().dependencyScope(lib.getName() + "JextractRuntimeOnly", config -> config.fromDependencyCollector(lib.getDependencies().getRuntimeOnly()));

                final NamedDomainObjectProvider<@NonNull ResolvableConfiguration> headerDirectoriesConfig = project.getConfigurations().resolvable(lib.getName() + "HeaderDirectories", config -> {
                    config.setDescription(String.format("The directories to search the %s library's public header file", lib.getName()));
                    config.extendsFrom(headerOnlyScope.get(), headerScope.get());
                    config.attributes(a -> {
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API));
                        a.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                    });
                });
                final NamedDomainObjectProvider<@NonNull ResolvableConfiguration> includePathConfig = project.getConfigurations().resolvable(lib.getName() + "IncludePath", config -> {
                    config.setDescription(String.format("The directories to be added to the %s library's include path", lib.getName()));
                    config.extendsFrom(includeOnlyScope.get(), includeScope.get());
                    config.attributes(a -> {
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API));
                        a.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                    });
                });
                project.getConfigurations().resolvable(lib.getName() + "LibraryPath", config -> {
                    config.setDescription(String.format("The directories to be added to the %s library's library path", lib.getName()));
                    config.extendsFrom(headerScope.get(), includeScope.get(), runtimeOnlyScope.get());
                    config.attributes(a -> {
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME));
                        a.attribute(Attribute.of("org.gradle.native.optimized", Boolean.class), true);
                        a.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                    });
                });

                final Provider<@NonNull RegularFile> headerFile = headerDirectoriesConfig.zip(lib.getDependencies().getHeaderFilter(), (config, patterns) -> {
                    return config.resolve().stream()
                        .map(d -> project.getLayout().getProjectDirectory().dir(d.getAbsolutePath()).getAsFileTree().matching(spec -> spec.include("**/*.h").include(patterns)).getFiles())
                        .flatMap(Collection::stream)
                        .map(f -> project.getLayout().getProjectDirectory().file(f.getAbsolutePath()))
                        .findFirst().orElse(null);
                });
                final Provider<@NonNull List<@NonNull Directory>> includeDirectories = includePathConfig.zip(lib.getIncludes(), (config, includes) -> {
                    return Stream.concat(
                        includes.stream(),
                            config.resolve().stream()
                                .map(d -> project.getLayout().getProjectDirectory().dir(d.getAbsolutePath()))
                    )
                        .collect(Collectors.toList());
                });
                project.getTasks().register(lib.getGenerateBindingsTaskName(), JextractGenerateTask.class, task -> {
                    task.setDescription("Uses Jextract to generate Java bindings for the " + lib.getName() + " native library");
                    task.getInstallation().convention(jextractInstallation);
                    task.getHeader().convention(lib.getHeader().orElse(headerFile));
                    task.getIncludes().convention(includeDirectories);
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
                    task.getHeader().convention(lib.getHeader().orElse(headerFile));
                    task.getIncludes().convention(includeDirectories);
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

        project.getPluginManager().withPlugin(GradleJmodPlugin.PLUGIN_NAME, jmodPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            javaExtension.getSourceSets().named("main", sourceSet -> {
                final var jmodExtension = sourceSet.getExtensions().getByType(JmodSourceSetExtension.class);
                final var jextractExtension = sourceSet.getExtensions().getByType(SourceSetExtension.class);
                jextractExtension.getLibraries().all(lib -> {
                    jmodExtension.getHeaders().srcDirs(lib.getIncludes(), project.getConfigurations().named(lib.getName() + "HeaderDirectories"));
                    jmodExtension.getBinaries().srcDirs(lib.getLibraryPath(), project.getConfigurations().named(lib.getName() + "LibraryPath"));
                    jmodExtension.getLegalNotices().srcDirs(lib.getLegalNotices());
                });
            });
        });
    }
}
