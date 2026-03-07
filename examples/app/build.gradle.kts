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
    // A library from a project dependency
    val hello by registering {
        dependencies {
            header(project(":nativelib"))
        }
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
