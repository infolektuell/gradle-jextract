# Gradle Jextract Plugin

This plugin enables developing Java code that makes use of native libraries using the new [Foreign Function & Memory API][ffm].
FFM API can be considered as a more modern and secure alternative to JNI for native access in Java.
The plugin generates Java bindings for native libraries using the FFM-related [Jextract] tool and makes them accessible in Gradle projects.

## Features

- [x] Downloads Jextract for the current build platform and architecture, no additional installation steps needed.
- [x] Preset conventions for Jextract versions 19 up to 22.
- [x] Download locations can be customized for more restrictive environments.
- [x] Alternately, use a local installation of Jextract.
- [x] Generated code is available in main sourceset of Java projects.
- [x] Configure multiple libs in one project
- [x] Compatible with [Configuration Cache]

## Usage

You might want to apply this plugin after one of the Java plugins.
It tries to get the correct Java version from the toolchain extension or uses the build JDK.
After running `./gradlew build`, the generated code will be available in the main source set.

### Simple Example

```
plugins {
    id("application")
    id("de.infolektuell.jextract") version "1.0.0"
}

jextract.libraries {
    create("bass") {
        header = layout.projectDirectory.file("bass.h")
        targetPackage = "com.un4seen.bass"
        headerClassName = "Bass"
        libraries = listOf("bass")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}
```

### Without Java Plugin

The extension offers a helper method to select a version without relying on a Java plugin.

```
plugins {
    id("de.infolektuell.jextract") version "1.0.0"
}

jextract {
    generator {
        javaVersion(JavaLanguageVersion.of(21)
    }
    libraries {
        ...
    }
}
```

### Custom Locations

You can select a preset before setting custom URLs to have sensible fallback conventions.
You have to add checksums for custom resources (SHA-256 by default).

```
plugins {
    id("de.infolektuell.jextract") version "1.0.0"
}

jextract {
    generator {
        javaVersion(JavaLanguageVersion.of(21)
        distribution {
            getByName("linux_x64") {
                url = "https://company.com/archives/jextract/21/jextract-linux-x64.tar.gz"
                checksum = "..."
            }
            getByName("mac_aarch64") {
                url = "https://company.com/archives/jextract/21/jextract-mac-aarch64.tar.gz"
                checksum = "..."
            }
            getByName("mac_x64") {
                url = "https://company.com/archives/jextract/21/jextract-mac-x64.tar.gz"
                checksum = "..."
            }
            getByName("windows_x64") {
                url = "https://company.com/archives/jextract/21/jextract-windows-x64.tar.gz"
                checksum = "..."
            }
        }
    }
    libraries {
        ...
    }
}
```

## License

[MIT License](LICENSE.txt)

[jextract]: https://jdk.java.net/jextract/
[ffm]: https://openjdk.org/jeps/454
[configuration cache]: https://docs.gradle.org/current/userguide/configuration_cache.html
