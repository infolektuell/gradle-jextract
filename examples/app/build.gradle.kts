plugins {
    id("de.infolektuell.jextract")
    id("de.infolektuell.jmod")
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

val nativeApi = dependencies.project(":nativelib", "cppApiElements")
val nativeRuntime = dependencies.project(":nativelib", "debugRuntimeElements")
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
            headerOnly(nativeApi)
            runtimeOnly(nativeRuntime)
        }
        headerClassName = "Hello"
        targetPackage = "de.infolektuell.hello.bindings"
        useSystemLoadLibrary = true // name guessed from library name
        libraries.add("nativelib")
    }

    sourceSets.main {
        jextract.libraries.addLater(hello)
        jextract.libraries.all {
            jmod.headers.srcDirs(configurations[name + "HeaderDirectories"])
            jmod.binaries.srcDirs(configurations[name + "LibraryPath"])
        }
    }
}

application {
    mainModule = "de.infolektuell.hello.app"
    mainClass = "de.infolektuell.hello.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.hello.app")
}

tasks.withType(Test::class) {
    doFirst {
        println("Starting the $name test task")
    }
}

tasks.named("run") {
    doFirst {
        println("Starting the $name task")
    }
}
