plugins {
    `java-library`
    id("de.infolektuell.jmod")
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
