package de.infolektuell.gradle.jmod;

import de.infolektuell.gradle.jmod.tasks.JmodExtractAction;
import de.infolektuell.gradle.jmod.tasks.LibraryPathProvider;
import de.infolektuell.gradle.jmod.extensions.JmodSourceSetExtension;
import de.infolektuell.gradle.jmod.tasks.JmodCreateTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.*;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
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
import java.util.Objects;

/// A Gradle plugin to add JMOD archive capabilities
@SuppressWarnings("UnstableApiUsage")
public abstract class GradleJmodPlugin implements Plugin<@NonNull Project> {
    /// The plugin ID that must be used in build scripts to apply the plugin
    public static final String PLUGIN_NAME = "de.infolektuell.jmod";

    public static abstract class JmodCompatibilityRule implements AttributeCompatibilityRule<@NonNull LibraryElements> {
        @Override
        public void execute(CompatibilityCheckDetails<@NonNull LibraryElements> details) {
            if (Objects.isNull(details.getConsumerValue()) || Objects.isNull(details.getProducerValue())) return;
            if (details.getProducerValue().getName().equals(LibraryElements.JAR) && details.getConsumerValue().getName().equals("jmod")) details.compatible();
            if (details.getProducerValue().getName().equals("jmod") && details.getConsumerValue().getName().equals(LibraryElements.DYNAMIC_LIB)) details.compatible();
        }
    }

    /// Used by Gradle
    public GradleJmodPlugin() { super(); }

    @Override
    public void apply(@NonNull Project project) {
        project.getPluginManager().withPlugin("java", javaPlugin -> {
            project.getDependencies().attributesSchema(schema -> schema.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, a -> a.getCompatibilityRules().add(JmodCompatibilityRule.class)));
            project.getDependencies().registerTransform(JmodExtractAction.class, transform -> {
                transform.getFrom().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                transform.getTo().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.CLASSES));
                transform.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jmod");
                transform.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JVM_CLASS_DIRECTORY);
                transform.getParameters().getPrefix().set(JmodExtractAction.Prefix.CLASSES);
            });
            project.getDependencies().registerTransform(JmodExtractAction.class, transform -> {
                transform.getFrom().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                transform.getTo().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.DYNAMIC_LIB));
                transform.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jmod");
                transform.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                transform.getParameters().getPrefix().set(JmodExtractAction.Prefix.LIB);
            });

            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final Provider<@NonNull JavaLanguageVersion> javaVersion = javaExtension.getToolchain().getLanguageVersion().orElse(JavaLanguageVersion.of(Objects.requireNonNullElse(Jvm.current().getJavaVersionMajor(), 25)));

            javaExtension.getSourceSets().configureEach(sourceSet -> {
                final JmodSourceSetExtension jmodSourceSetExtension = project.getObjects().newInstance(JmodSourceSetExtension.class);
                sourceSet.getExtensions().add("jmod", jmodSourceSetExtension);
            });

            javaExtension.getSourceSets().named("main", sourceSet -> {
                final JmodSourceSetExtension jmodSourceSetExtension = sourceSet.getExtensions().getByType(JmodSourceSetExtension.class);
                project.getConfigurations().resolvable(jmodSourceSetExtension.getLibraryPathConfigurationName(), config -> {
                    config.extendsFrom(project.getConfigurations().getByName(sourceSet.getImplementationConfigurationName()), project.getConfigurations().getByName(sourceSet.getRuntimeOnlyConfigurationName()));
                    config.attributes(attributes -> {
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.DYNAMIC_LIB));
                        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                        attributes.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaExtension.getToolchain().getLanguageVersion().map(JavaLanguageVersion::asInt));
                        attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
                    });
                });

                final Provider<@NonNull Boolean> isModularProject = sourceSet.getJava().getSourceDirectories().filter(this::isModule).getElements().map(e -> !e.isEmpty());
                final TaskProvider<@NonNull Jar> jarTask = project.getTasks().named(sourceSet.getJarTaskName(), Jar.class);
                final TaskProvider<@NonNull JmodCreateTask> createJmodTask = project.getTasks().register(sourceSet.getTaskName("create", "Jmod"), JmodCreateTask.class, task -> {
                    task.setGroup("build");
                    task.setDescription("Assembles a jmod archive containing the classes of the 'main' feature.");
                    task.onlyIf(t -> isModularProject.get());
                    task.getMetadata().convention(javaVersion.flatMap(v -> getJavaToolchainService().compilerFor(spec -> spec.getLanguageVersion().set(v)).map(JavaCompiler::getMetadata)));
                    task.getClasspath().from(jarTask);
                    task.getJmod().convention(jarTask.flatMap(t -> t.getDestinationDirectory().map(d -> d.file(project.getName() + ".jmod"))));
                    task.getHeaderFiles().from(jmodSourceSetExtension.getHeaders().getSourceDirectories());
                    task.getLibs().from(jmodSourceSetExtension.getBinaries().getSourceDirectories());
                    task.getLegalNotices().from(jmodSourceSetExtension.getLegalNotices().getSourceDirectories());
                });

                project.getConfigurations().consumable(jmodSourceSetExtension.getApiElementsConfigurationName(), config -> {
                    config.attributes(attributes -> {
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_API));
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                        attributes.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaExtension.getToolchain().getLanguageVersion().map(JavaLanguageVersion::asInt));
                        attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
                    });
                    config.getOutgoing().artifact(createJmodTask, artifact -> artifact.setType("jmod"));
                });
                project.getConfigurations().consumable(jmodSourceSetExtension.getRuntimeElementsConfigurationName(), config -> {
                    config.attributes(attributes -> {
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jmod"));
                        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                        attributes.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaExtension.getToolchain().getLanguageVersion().map(JavaLanguageVersion::asInt));
                        attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
                    });
                    config.getOutgoing().artifact(createJmodTask, artifact -> artifact.setType("jmod"));
                });
            });

                javaExtension.getSourceSets().named("test", sourceSet -> {
                    final JmodSourceSetExtension jmodSourceSetExtension = sourceSet.getExtensions().getByType(JmodSourceSetExtension.class);
                    final var mainBinaries = javaExtension.getSourceSets().named("main").map(s -> s.getExtensions().getByType(JmodSourceSetExtension.class).getBinaries().getSourceDirectories());
                    final LibraryPathProvider libraryPathProvider = project.getObjects().newInstance(LibraryPathProvider.class);
                    libraryPathProvider.getFiles().from(jmodSourceSetExtension.getBinaries().getSourceDirectories());
                    libraryPathProvider.getFiles().from(mainBinaries);
                    libraryPathProvider.getFiles().from(project.getConfigurations().named(jmodSourceSetExtension.getLibraryPathConfigurationName()));
                    project.getTasks().withType(Test.class, task -> {
                        task.getJvmArgumentProviders().add(libraryPathProvider);
                    });
                });
        });

        project.getPluginManager().withPlugin("application", applicationPlugin -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final JavaApplication application = project.getExtensions().getByType(JavaApplication.class);
            javaExtension.getSourceSets().named("main", sourceSet -> {
                project.getTasks().named(sourceSet.getTaskName("create", "jmod"), JmodCreateTask.class, task -> {
                    task.getMainClass().convention(application.getMainClass());
                });
                final JmodSourceSetExtension jmodSourceSetExtension = sourceSet.getExtensions().getByType(JmodSourceSetExtension.class);
                final LibraryPathProvider libraryPathProvider = project.getObjects().newInstance(LibraryPathProvider.class);
                libraryPathProvider.getFiles().from(jmodSourceSetExtension.getBinaries().getSourceDirectories());
                libraryPathProvider.getFiles().from(project.getConfigurations().named(jmodSourceSetExtension.getLibraryPathConfigurationName()));
                project.getTasks().named("run", JavaExec.class, task -> {
                    task.getJvmArgumentProviders().add(libraryPathProvider);
                });
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
