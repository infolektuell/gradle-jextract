# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Replace the nested resource property of DownloadTask with a property that receives an instance of the Resource class from a separate business model.
  The values of these resources strictly belong together, so a data class coming from a business model is more appropriate than a bean with separate properties. 
- The plugin adds an extension to each source set where Jextract libraries can be added.
  This breaking API change improves flexibility and stability.

### Added

- Add output properties in DSL extension to customize the output path per library or as a parent folder for all libraries without a specified output path.
  If a library has no explicit output path, its name is used to create a subdirectory under the top-level output path.
  If no explicit top-level output is given, it defaults to a convention in the build directory.

### Fixed

- Check for the Java toolchain version only if and after the Java plugin is applied.
- Pass the `--use-system-load-library` command line flag only to Jextract 22 or above, fails with Jextract 21 and below.

### Removed

- Remove The sourceSet property in the DSL which is superseded by a source set extension.

## [0.4.0] - 2024-11-08

### Changed

- When Jextract 22 is requested, Build 22-jextract+6-47 will be downloaded from now on.

### Fixed

- Improved configuration cache stability by moving provider creation to task action.

## [0.3.0] - 2024-10-12

### Added

- Create a changelog file for release notes
- The source set the generated sources are added to is now user-selectable via extension property, `main` is chosen by default.
- Tasks became cacheable, so the downloaded Jextract archives and generated sources can be shared and re-used via build cache.

### Changed

- Jextract is downloaded and installed to the project build directory to avoid access conflicts for output files and directories in multi-project builds.

## [0.2.1] - 2024-09-28

### Fixed

- Make DownloadTask compatible with the configuration cache.

## [0.2.0] - 2024-09-27

### Added

- Support argFile configuration as an alternative filtering mechanism to setting included symbols directly in the extension, use `whitelist.argFile` property in the library config.
- Add `DumpIncludes` task to dump includes encountered in a library's headers to a text file in the build directory per convention: `gradlew dumpIncludes`.
  This file can be copied and modified for further usage as argFile.

### Changed

- Parallelize tasks running Jextract for faster multi-lib builds.

### Fixed

- Use Jextract 22 for JVM 23.

### Removed

- Remove the custom download location DSL, but the download task can be configured with custom URLs.
- Remove the separate download plugin and merge download stuff into this plugin.

## [0.1.0] - 2024-09-16

### Added

- Tasks to download and install Jextract from the official EA download page
- Task to generate bindings for given headers using Jextract
- DSL extension to configure multiple native libraries and desired Jextract version/installation

[unreleased]: https://github.com/infolektuell/gradle-jextract/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/infolektuell/gradle-jextract/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/infolektuell/gradle-jextract/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/infolektuell/gradle-jextract/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/infolektuell/gradle-jextract/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/infolektuell/gradle-jextract/releases/tag/v0.1.0
