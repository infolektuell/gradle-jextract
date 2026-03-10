package de.infolektuell.gradle.jmod.extensions;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

/// Source set DSL extension to configure additional source directories for JMOD archive content
public abstract class JmodSourceSetExtension {
    private final SourceDirectorySet headers = getObjects().sourceDirectorySet("headers", "Native Header Files");
    private final SourceDirectorySet binaries = getObjects().sourceDirectorySet("binaries", "Native Binaries");
    private final SourceDirectorySet legalNotices = getObjects().sourceDirectorySet("legal", "Legal Notices for JMOD Content");

    /// Used by gradle
    public JmodSourceSetExtension() { super(); }

    ///  Source directories for native header files
    /// @return A configurable source directory set
    public SourceDirectorySet getHeaders() { return headers; }

    ///  Source directories for native binary files
    /// @return A configurable source directory set
    public SourceDirectorySet getBinaries() { return binaries; }

    ///  Source directories for legal notices
    /// @return A configurable source directory set
    public SourceDirectorySet getLegalNotices() { return legalNotices; }

    /// The name of the configuration that collects the binaries from resolved JMOD files
    public String getLibraryPathConfigurationName() { return "jmodLibraryPath"; }
    /// The name of the configuration that exposes a consumable JMOD archive for API usage
    public String getApiElementsConfigurationName() { return "apiJmodElements"; }
    /// The name of the configuration that exposes a consumable JMOD archive for API usage
    public String getRuntimeElementsConfigurationName() { return "runtimeJmodElements"; }

    /// Inject the object factory build service
    /// @return The injected build service
    @Inject
    protected abstract ObjectFactory getObjects();
}
