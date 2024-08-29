plugins {
    id("application")
    id("de.infolektuell.jextract")
}

jextract {
    generator {
        javaVersion(JavaLanguageVersion.of(21))
    }
    useSystemLoadLibrary = true
    libraries {
        create("bass") {
            header = layout.projectDirectory.file("bass.h")
        }
    }
}
