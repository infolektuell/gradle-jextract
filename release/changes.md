- Replace the nested resource property of DownloadTask with a property that receives an instance of the Resource class from a separate business model.
  The values of these resources strictly belong together, so a data class coming from a business model is more appropriate than a bean with separate properties.
- The plugin adds an extension to each source set where Jextract libraries can be added.
  This breaking API change improves flexibility and stability.
- Add output properties in DSL extension to customize the output path per library or as a parent folder for all libraries without a specified output path.
  If a library has no explicit output path, its name is used to create a subdirectory under the top-level output path.
  If no explicit top-level output is given, it defaults to a convention in the build directory.
- Add DSL properties to configure Jextract 21 to generate source files instead of class files (per library or top-level).
- Check for the Java toolchain version only if and after the Java plugin is applied.
- Pass the `--use-system-load-library` command line flag only to Jextract 22 or above, fails with Jextract 21 and below.
- Remove The sourceSet property in the DSL which is superseded by a source set extension.
