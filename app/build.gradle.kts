plugins {
    id("application")
    id("de.infolektuell.jextract")
}

jextract {
    generator = file(System.getProperty("user.home")).resolve("bin")
    libraries {
        create("bass") {
            header = layout.projectDirectory.file("bass.h")
        }
    }
}
