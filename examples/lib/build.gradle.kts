plugins {
    id("common-conventions")
}

jextract.libraries {
    // The native BASS audio library.
    val bass by registering {
        // output = layout.projectDirectory.dir("bassBindings")
        header = layout.projectDirectory.file("src/main/public/bass.h")
        headerClassName = "Bass"
        targetPackage = "com.un4seen.bass"
        // Make your public headers folder searchable for Jextract
        includes.add(layout.projectDirectory.dir("src/main/public"))
        // For large headers it is good practice to generate only the symbols you need.
        whitelist {
            // We only want to access the BASS version
            functions.add("BASS_GetVersion")
        }
    }
    sourceSets.named("main") {
            jextract.libraries.addLater(bass)
    }
}
