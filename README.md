# Gradle Jextract Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/de.infolektuell.jextract)](https://plugins.gradle.org/plugin/de.infolektuell.jextract)

This Gradle plugin adds [Jextract] to a Java library or application project. It generates Java bindings for the configured native libraries and adds them to the respective source set.

The compiled classes, headers, native binaries and more can also packaged in a JMOD archive. This enables integration with Jlink for creating applications and libraries with native access without having to extract the binaries from JAR resources.

## Quick Start

```kts
plugins {
    `java-library`
    id("de.infolektuell.jextract") version "x.y.z"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets.main {
  jextract.libraries.register("greeting") {
    headers = listOf("greeting.h") // Default convention
    libraries = listOf("greeting") // Default convention
    headerClassName = "Greeting"
    targetPackage = "com.example.greeting"
    // Directories to search for header files (convention)
    includePath = listOf(layout.projectDirectory.dir("src/main/public"))
    // Directories containing native binary files, needed for use-system-load-library or if a working JMOD archive should be created.
    libraryPath = listOf(layout.projectDirectory.dir("src/main/lib"))
    useSystemLoadLibrary = true
  }
}
```

Please visit the [Setup guide] on the documentation website for more details.

## Change history

See GitHub Releases or the [changelog file](CHANGELOG.md) for releases and changes.

## License

[MIT License](LICENSE.txt)

[jextract]: https://jdk.java.net/jextract/
[setup guide]: https://infolektuell.github.io/gradle-jextract/start/setup/
