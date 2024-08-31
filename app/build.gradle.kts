plugins {
    id("application")
    id("de.infolektuell.jextract")
}

jextract {
    generator {
        javaVersion(JavaLanguageVersion.of(21))
    }
    libraries {
        create("bass") {
            header = layout.projectDirectory.file("bass.h")
        }
    }
}
