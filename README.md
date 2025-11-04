# Gradle Jextract Plugin

[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/de.infolektuell.jextract)](https://plugins.gradle.org/plugin/de.infolektuell.jextract)

This is a Gradle plugin that adds [Jextract] to a Gradle build. 

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

jextract.libraries {
  val greeting by registering {
    header = layout.projectDirectory.file("src/main/public/greeting.h")
    headerClassName = "Greeting"
    targetPackage = "com.example.greeting"
    useSystemLoadLibrary = true
    libraries.add("greeting")
  }
  sourceSets.named("main") {
    jextract.libraries.addLater(greeting)
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
