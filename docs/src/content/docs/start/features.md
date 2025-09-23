---
title: Introduction
description: The features offered by the Gradle Jextract plugin
sidebar:
  order: 1
---

This plugin enables developing Java code that makes use of native libraries using the new [Foreign Function & Memory API][ffm].

- FFM API can be considered as a more modern and secure alternative to JNI for native access in Java.
- The FFM-related [Jextract] tool reads C header files and generates Java bindings for statically typed native access. These are Java classes to be used in a Java project.
- This Gradle plugin runs Jextract on your header files and makes the generated bindings available for your source code.

## Features

### Requirements

This plugin was created to get rid of a local Jextract installation requirement and to retain the “Clone&Build” Gradle experience.
All you need is at least JVM 17 and your project should use at least Gradle v8.8.

- Downloads Jextract for the current build platform and architecture, no additional installation steps are needed.
- Preset conventions for Jextract versions 19 up to 22.
- Download locations can be customized for more restrictive environments.
- Alternately, use a local installation of Jextract.

### Library configuration

All features are configurable via script DSL extension.
So importing and configuring tasks directly is not necessary.

- Configure as many libs as needed.
- Customize the output path for each lib
- Add the generated code to any Java source sets.
- Filter which symbols in your lib should be included, via DSL or arg file.
- Dump all includes into an arg file for further usage and filtering.

### Gradle

The plugin tries to stay up-to-date with Gradle's development and benefits from their performance optimization.

- Compatible with [Configuration Cache].
- Tasks are [cacheable][build cache].

[jextract]: https://jdk.java.net/jextract/
[ffm]: https://openjdk.org/jeps/454
[configuration cache]: https://docs.gradle.org/current/userguide/configuration_cache.html
[build cache]: https://docs.gradle.org/current/userguide/build_cache.html
