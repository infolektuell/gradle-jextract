plugins {
    id("de.infolektuell.jextract")
    application
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

jextract.libraries {
    // The native BASS audio library.
    val bass by registering {
        header = layout.projectDirectory.file("src/main/public/bass.h")
        headerClassName = "Bass"
        targetPackage = "com.un4seen.bass"
        // For large headers it is good practice to generate only the symbols you need.
        whitelist {
            // We only want to access the BASS version
            functions.add("BASS_GetVersion")
        }
        useSystemLoadLibrary = true // name guessed from library name
        libraryPath.add(findLibraryPath().get())
        legalNotices.add(layout.projectDirectory.dir("src/main/lib/legal"))
    }

    sourceSets.main {
        jextract.libraries.addLater(bass)
    }
}

application {
    mainModule = "de.infolektuell.bass.app"
    mainClass = "de.infolektuell.bass.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.bass.app")
}

fun findLibraryPath() = providers.systemProperty("os.name").map {
    val basePath = layout.projectDirectory.dir("src/main/lib")
    if (it.contains("windows", true)) basePath.dir("windows/x64")
    else if (it.contains("mac", true)) basePath.dir("macos/x64")
    else basePath.dir("linux/x64")
}
