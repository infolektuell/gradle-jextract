# Gradle Jextract Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/de.infolektuell.jextract)](https://plugins.gradle.org/plugin/de.infolektuell.jextract)

This plugin enables developing Java code that makes use of native libraries using the new [Foreign Function & Memory API][ffm].
FFM API can be considered as a more modern and secure alternative to JNI for native access in Java.
The plugin generates Java bindings for native libraries using the FFM-related [Jextract] tool and makes them accessible in java compilation.

## Features

- [x] Downloads Jextract for the current build platform and architecture, no additional installation steps needed.
- [x] Preset conventions for Jextract versions 19 up to 22.
- [x] Download locations can be customized for more restrictive environments.
- [x] Alternately, use a local installation of Jextract.
- [x] Generated source code is available in a user-selected Java source set (`main` by default).
- [x] Multiple libs can be configured in one project and are built concurrently.
- [x] Whitelisting of included symbols via DSL extension
- [x] Can dump includes and use arg files
- [x] Compatible with [Configuration Cache].
- [x] Tasks are [cacheable][build cache].

## Usage

You might want to apply this plugin after one of the Java plugins.
It tries to get the correct Java version from the toolchain extension or falls back to the JDK Gradle is running on.
On JDk 23, Jextract 22 is used, because FFM has been finalized, so there are effectively no differences between version 22 and 23.
After running `./gradlew build`, the generated code will be available in the main source set.

### Simple Example

In _build.gradle.kts_:

```gradle kotlin dsl
plugins {
    id("application")
    id("de.infolektuell.jextract") version "0.4.0"
}

jextract.libraries {
    create("bass") {
        header = layout.projectDirectory.file("bass.h")
        targetPackage = "com.un4seen.bass"
        headerClassName = "Bass"
        includes.add(layout.projectDirectory.dir("src/main/public"))
        libraries = listOf("bass")
        useSystemLoadLibrary = true
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}
```

### Filtering and arg files

The plugin can dump includes encountered in the headers and use arg files.
Running `gradlew dumpIncludes` produces a _<libname>-includes.txt_ file for each configured lib under _build/reports/jextract/_.
To use this as an arg file for storing your whitelisted includes, you should copy, modify, and put it under source control.
Add this copy to the plugin's extension.
Instead of using an arg file, included symbols can also be configured directly in the whitelist extension via respective properties.

```gradle kotlin dsl
jextract.libraries {
    create("bass") {
        header = layout.projectDirectory.file("bass.h")
        targetPackage = "com.un4seen.bass"
        headerClassName = "Bass"
            whitelist {
                argFile = layout.projectDirectory.file("src/main/includes/bass-includes.txt")
                // Include more symbols via DSL extension
                functions.addAll("bass_get_version")
            }
        libraries = listOf("bass")
    }
}
```

See [Filtering] section in the Jextract guide for more information.

### Without Java Plugin

The extension offers a property to select a version without relying on a Java plugin.

```gradle kotlin dsl
jextract {
    generator {
        javaLanguageVersion = JavaLanguageVersion.of(21)
    }
}
```

### Local Installation

Instead of downloading Jextract, a local installation directory can be configured.

```gradle kotlin dsl
jextract {
    generator {
        local = layout.projectDirectory.dir("/usr/local/opt/jextract-22/") 
    }
}
```

### Custom Locations

Jextract is downloaded from the [official page][jextract] by default.
If custom locations are needed, the download task must be configured (url, checksum, and verification algorithm).
The plugin implements its own decision logic to select appropriate conventions depending on the current build platform and JDK version.

```gradle kotlin dsl
import de.infolektuell.gradle.jextract.tasks.DownloadTask

tasks.withType(DownloadTask::class).configureEach {
    resource.url = uri("https://my-company.com/jextract/file.tgz")
    resource.checksum = "xyz"
    resource.algorithm = "SHA-512" // SHA-256 by default
}
```

### Custom source set

In some situations, the `main` source set is not available or the sources should be added to another one, e.g., `test`.
This must be configured in the extension:

```gradle kotlin dsl
jextract {
    sourceSet = sourceSets.named("test")
}
```

## License

[MIT License](LICENSE.txt)

[jextract]: https://jdk.java.net/jextract/
[ffm]: https://openjdk.org/jeps/454
[configuration cache]: https://docs.gradle.org/current/userguide/configuration_cache.html
[build cache]: https://docs.gradle.org/current/userguide/build_cache.html
[filtering]: https://github.com/openjdk/jextract/blob/master/doc/GUIDE.md#filtering
