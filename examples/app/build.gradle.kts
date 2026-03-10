plugins {
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

dependencies {
    implementation(project(":lib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

application {
    mainModule = "de.infolektuell.hello.app"
    mainClass = "de.infolektuell.hello.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.hello")
}
