plugins {
    `java-library`
    id("de.infolektuell.jextract")
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
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
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
