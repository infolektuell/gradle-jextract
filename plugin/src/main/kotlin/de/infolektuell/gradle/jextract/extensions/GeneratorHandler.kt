package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLanguageVersion

interface GeneratorHandler {
    /** Explicitly set Java version to download Jextract for, defaults to JVM toolchain */
    val javaLanguageVersion: Property<JavaLanguageVersion>
    /** A [properties][java.util.Properties] file containing the remote locations where to download the Jextract distributions */
    val distributions: RegularFileProperty
    /** A directory containing a JExtract installation */
    val local: DirectoryProperty
}
