# Gradle Jextract Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/de.infolektuell.jextract)](https://plugins.gradle.org/plugin/de.infolektuell.jextract)

This plugin enables developing Java code that makes use of native libraries using the new [Foreign Function & Memory API][ffm].
FFM API can be considered as a more modern and secure alternative to JNI for native access in Java.
The plugin generates Java bindings for native libraries using the FFM-related [Jextract] tool and makes them accessible in java compilation.

## Features

- [x] Downloads Jextract for the current build platform and architecture, no additional installation steps are needed.
- [x] Preset conventions for Jextract versions 19 up to 22.
- [x] Download locations can be customized for more restrictive environments.
- [x] Alternately, use a local installation of Jextract.
- [x] Multiple libs can be configured in one project and are built concurrently.
- [x] Customize the output path for each lib
- [x] Configurable to add generated lib outputs to Java source sets
- [x] Whitelisting of included symbols via DSL extension
- [x] Can dump includes and use arg files
- [x] Compatible with [Configuration Cache].
- [x] Tasks are [cacheable][build cache].

The Java plugin is not required, but this plugin reacts to it.
On JDk 23, Jextract 22 is used because FFM has been finalized.

## Usage

Include the plugin in _build.gradle.kts_:

```gradle kotlin dsl
plugins {
    id("base")
    id("de.infolektuell.jextract") version "0.5.0"
}
```

Running `gradlew jextract` will download and install Jextract into the build folder, but no libs are defined to generate bindings for.
The matching Jextract distribution is downloaded from the [official page][jextract] by default.

No Java plugin is applied, so the JVM version Gradle is running on is selected.
The version can be set explicitly using the plugin DSL extension:

```gradle kotlin dsl
jextract.generator.javaLanguageVersion = JavaLanguageVersion.of(21)
```

### Defining libraries

Jextract generates Java bindings for native libraries consisting of public headers and binaries.
These must be defined using the plugin's DSL.
This example demonstrates some Jextract-specific settings.
Append this in _build.gradle.kts_:

```gradle kotlin dsl
jextract.libraries {
        // The native BASS audio library.
        val bass by registering {
            header = layout.projectDirectory.file("src/main/public/bass.h")
            headerClassName = "Bass"
            targetPackage = "com.un4seen.bass"
            useSystemLoadLibrary = true
            libraries.add("bass")
            // Make your public headers folder searchable for Jextract
            includes.add(layout.projectDirectory.dir("src/main/public"))
            // For large headers it is good practice to generate only the symbols you need.
            whitelist {
                // We only want to access the BASS version
                functions.addAll("bass_get_version")
            }
        }
}
```

You can define as many libraries as you need.
Running `gradlew jextract` will generate bindings for the defined libraries into the conventional output path under _build/generated/sources/jextract/<libname>_.

### Custom output path

There are flexible options to customize the output path.

#### Customizing the parent folder

```gradle kotlin dsl
jextract.output = layout.projectDirectory.dir("bindings")
```

Running `gradlew jextract` will generate bindings for the defined libraries into the customized output path under _bindings/<libname>_.

#### Customizing a library's output path

The output path of each library is customizable.
The top-level output path is ignored by libraries with an explicit output path

```gradle kotlin dsl
jextract.libraries {
    val bass by registering {
        output = layout.projectDirectory.dir("bassBindings")
    }
}
```

### Filtering and arg files

See [Filtering] in the Jextract guide for more information.
The whitelist DSL enables symbol filtering in the build script, but using arg files is possible too. Both can be combined.

1. Run `gradlew dumpIncludes` to Generate an arg file for each library under _build/reports/jextract/<libname>-includes.txt_.
2. Copy the desired file to src or a similar place where it should be persisted.
3. Modify your copy, remove unnecessary symbols, keep what you need.
4. Add the file to the build script using the DSL.

```gradle kotlin dsl
jextract.libraries {
    create("bass") {
            whitelist {
                argFile = layout.projectDirectory.file("src/main/includes/bass-includes.txt")
            }
    }
}
```

### Local Installation

To run a local Jextract installation instead of downloading it, the location must be given.
The correct version should be set too.

```gradle kotlin dsl
jextract.generator {
    local = layout.projectDirectory.dir("/usr/local/opt/jextract-22/")
    javaLanguageVersion = JavaLanguageVersion.of(21) 
}
```

### Custom Download Locations

If custom locations are needed, the download task needs a resource object with url, checksum, and verification algorithm (SHA-256 by default).
In practice, you will have to implement platform-specific resources.

```gradle kotlin dsl
import de.infolektuell.gradle.jextract.JextractDataStore.Resource
import de.infolektuell.gradle.jextract.tasks.DownloadTask

tasks.withType(DownloadTask::class).configureEach {
    val version = 22
    val url = uri("https://my-company.com/jextract/file.tgz")
    val checksum = "abcdef123456"
    resource = Resource(version, url, checksum, "SHA-512")
}
```

### Generating Source Files with older Jextract Versions

Jextract 22 generates Java source files by default, but Jextract 21 generates class files and needs the respective command line flag to generate source files.
In the plugin DSL extension this can be configured top-level or per library.

```gradle kotlin dsl
jextract.generator.javaLanguageVersion = JavaLanguageVersion.of(21)
jextract.generateSourceFiles = true
```

This was a preview feature. Maybe you want to upgrade instead of using this.

## Using the generated bindings

If the Java plugin is present, the Jextract version is determined based on the Java toolchain version.
It works with any plugin that applies the Java plugin.

```gradle kotlin dsl
plugins {
    `java-library`
    id("de.infolektuell.jextract") version "0.5.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

The source sets for Java are not aware of the generated bindings.
This plugin adds a `jextract` extension to each source set.
Jextract library definitions can be added to this extension and will be included in the Java compilation.

```gradle kotlin dsl
jextract.libraries {
        val bass by registering {}
        sourceSets {
            named("main") { // jvmMain for KMP
                jextract.libraries.addLater(bass)
            }
        }
}
```

## License

[MIT License](LICENSE.txt)

[jextract]: https://jdk.java.net/jextract/
[ffm]: https://openjdk.org/jeps/454
[configuration cache]: https://docs.gradle.org/current/userguide/configuration_cache.html
[build cache]: https://docs.gradle.org/current/userguide/build_cache.html
[filtering]: https://github.com/openjdk/jextract/blob/master/doc/GUIDE.md#filtering
