plugins {
    id("de.infolektuell.jextract")
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jextract.libraries {
    // The native BASS audio library.
    val bass by registering {
        headerClassName = "Bass"
        targetPackage = "com.un4seen.bass"
        // For large headers it is good practice to generate only the symbols you need.
        whitelist {
            // We only want to access the BASS version
            functions.add("BASS_GetVersion")
        }
        useSystemLoadLibrary = true
    }

    sourceSets.named("main") {
            jextract {
                libraries.addLater(bass)
                binaries.setSrcDirs(listOf(findLibraries("bass").get()))
            }
    }
}

application {
    mainModule = "de.infolektuell.bass"
    mainClass = "de.infolektuell.bass.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.bass")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun findLibraries(name: String): Provider<Directory> {
    return providers.systemProperty("os.name").map { os ->
        val osPart = if (os.contains("windows", true)) "windows"
        else if (os.contains("mac", true)) "macos"
        else "linux"
        layout.projectDirectory.dir("src/main/lib/${name}/${osPart}/x64")
    }
}
