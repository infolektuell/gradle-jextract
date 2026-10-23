package de.infolektuell.gradle.jextract.extensions;

import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.artifacts.dsl.DependencyCollector;

///  Custom dependencies for native libraries to generate bindings for using Jextract
public interface JextractLibraryDependencies extends Dependencies {
    /// Dependencies that are only searched for public headers to generate bindings for
    DependencyCollector getHeaderOnly();

    /// Dependencies that are searched for headers to generate bindings for and for binaries belonging to these headers
    DependencyCollector getHeader();

    /// Dependencies that are only used as include path directories
    DependencyCollector getIncludeOnly();

    /// Dependencies that are searched for include path directories and for binaries belonging to these headers
    DependencyCollector getInclude();

    /// Dependencies that are searched for binaries to be loaded on runtime
    DependencyCollector getRuntimeOnly();
}
