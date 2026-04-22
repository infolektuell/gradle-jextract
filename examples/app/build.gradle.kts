plugins {
    id("de.infolektuell.jmod") version "0.1.0"
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

// Must create project dependency from non-custom dependencies block
val nativelib = dependencies.project(":nativelib")
jmod.dependencies {
    // Use native headers during compile and binaries during runtime phase
    nativeImplementation(nativelib)
}

jextract.libraries {
    val headerFile = configurations["nativeApi"]
        .map { fileTree(it).matching { include("**/hello.h") } }
        .find { !it.isEmpty }
        ?.singleFile

    // A library from a project dependency
    val hello by registering {
        header = headerFile
        headerClassName = "Hello"
        targetPackage = "de.infolektuell.hello.bindings"
        useSystemLoadLibrary = true // name guessed from library name
        libraries.add("nativelib")
    }

    sourceSets.main {
        jextract.libraries.addLater(hello)
    }
}

application {
    mainModule = "de.infolektuell.hello.app"
    mainClass = "de.infolektuell.hello.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.hello.app")
}
