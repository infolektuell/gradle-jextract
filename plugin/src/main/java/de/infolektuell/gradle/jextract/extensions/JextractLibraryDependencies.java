package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jspecify.annotations.NonNull;

///  Custom dependencies for native libraries to generate bindings for using Jextract
public interface JextractLibraryDependencies extends Dependencies {
    /// Dependencies that are only searched for public headers to generate bindings for
    /// @return a configurable dependency collector
    DependencyCollector getHeaderOnly();

    /// Dependencies that are searched for headers to generate bindings for and for binaries belonging to these headers
    /// @return a configurable dependency collector
    DependencyCollector getHeader();

    ///  A set of Ant-style include patterns to filter which header files Jextract should generate bindings for.
    /// Per convention, a pattern is guessed from the configured library name: `**/<libname>.h`.
    /// @return The set property to configure the include patterns.
    /// @see org.gradle.api.tasks.util.PatternFilterable
    SetProperty<@NonNull String> getHeaderFilter();

    /// Dependencies that are only used as include path directories
    /// @return a configurable dependency collector
    DependencyCollector getIncludeOnly();

    /// Dependencies that are searched for include path directories and for binaries belonging to these headers
    /// @return a configurable dependency collector
    DependencyCollector getInclude();

    /// Dependencies that are searched for binaries to be loaded on runtime
    /// @return a configurable dependency collector
    DependencyCollector getRuntimeOnly();
}
