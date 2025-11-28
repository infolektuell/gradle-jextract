package de.infolektuell.gradle.jextract;

import de.infolektuell.gradle.jextract.extensions.JextractExtension;
import de.infolektuell.gradle.jextract.extensions.SourceSetExtension;
import de.infolektuell.gradle.jextract.service.JextractStore;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask.LocalJextractInstallation;
import de.infolektuell.gradle.jextract.tasks.JextractBaseTask.RemoteJextractInstallation;
import de.infolektuell.gradle.jextract.tasks.JextractDumpIncludesTask;
import de.infolektuell.gradle.jextract.tasks.JextractGenerateTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GradleJextractPlugin implements Plugin<@NonNull Project> {
    public static final String PLUGIN_NAME = "de.infolektuell.jextract";

    public void apply(Project project) {
        final JextractExtension extension = project.getExtensions().create(JextractExtension.EXTENSION_NAME, JextractExtension.class);
        extension.getInstallation().getJavaLanguageVersion().convention(JavaLanguageVersion.of(Objects.requireNonNullElse(Jvm.current().getJavaVersionMajor(), 25)));
        extension.getOutput().convention(project.getLayout().getBuildDirectory().dir("generated/sources/jextract"));
        extension.getGenerateSourceFiles().convention(false);

        project.getGradle().getSharedServices().registerIfAbsent(JextractStore.SERVICE_NAME, JextractStore.class, s -> {
            s.getMaxParallelUsages().convention(1);
            s.parameters(p -> {
                p.getCacheDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir(".gradle/jextract"));
                p.getDistributions().convention(extension.getDistributions());
            });
        });

        extension.getLibraries().configureEach(lib -> {
            lib.getUseSystemLoadLibrary().convention(false);
            lib.getOutput().convention(extension.getOutput().dir(lib.getName()));
            lib.getGenerateSourceFiles().convention(extension.getGenerateSourceFiles());
        });

        project.getTasks().withType(JextractBaseTask.class, task -> {
            if (extension.getInstallation().getLocation().isPresent()) {
                var installation = project.getObjects().newInstance(LocalJextractInstallation.class);
                installation.getLocation().convention(extension.getInstallation().getLocation());
                task.getInstallation().set(installation);
            } else {
                var installation = project.getObjects().newInstance(RemoteJextractInstallation.class);
                installation.getJavaLanguageVersion().convention(extension.getInstallation().getJavaLanguageVersion());
                task.getInstallation().set(installation);
            }
        });

        final Map<String, TaskProvider<@NonNull JextractGenerateTask>> jextractGenerateTasks = new HashMap<>();
        extension.getLibraries().all(lib -> {
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

        project.getPluginManager().withPlugin("java", p -> {
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            extension.getInstallation().getJavaLanguageVersion().convention(javaExtension.getToolchain().getLanguageVersion());
            project.getExtensions().getByType(SourceSetContainer.class).all((s -> {
                final SourceSetExtension sourceSetExtension = s.getExtensions().create(SourceSetExtension.EXTENSION_NAME, SourceSetExtension.class, project.getObjects());
                sourceSetExtension.getLibraries().all(lib -> {
                    final Provider<@NonNull Directory> src = jextractGenerateTasks.get(lib.getName()).flatMap(JextractGenerateTask::getSources);
                    s.getJava().srcDir(src);
                    s.getResources().srcDir(src);
                    s.setCompileClasspath(s.getCompileClasspath().plus(project.getLayout().files(src)));
                    s.setRuntimeClasspath(s.getRuntimeClasspath().plus(project.getLayout().files(src)));
                });
            }));
        });
    }
}
