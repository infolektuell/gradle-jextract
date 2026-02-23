# Jextract examples

This project contains two examples:

- [app](./app/) Is a Java application that uses `System.loadLibrary` to load its native libraries from the library search path. This setup is less robust for standalone deployment, but useful for packaging the app using Jlink or Jpackage. Loading is more performant and the app becomes smaller.
- [lib](./lib/) is a Java library that includes its native binaries as resources and loads them by extracting and loading the files at runtime. This setup is more robust for deployment, but less performant.
