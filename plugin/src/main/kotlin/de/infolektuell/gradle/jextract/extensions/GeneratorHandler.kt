package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLanguageVersion

abstract class GeneratorHandler {
    /** Explicitly set Java version to download Jextract for, defaults to JVM toolchain */
    abstract val javaLanguageVersion: Property<JavaLanguageVersion>
    /** A directory containing a JExtract installation */
    abstract val local: DirectoryProperty
}
