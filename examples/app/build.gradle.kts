plugins {
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
    mainModule = "de.infolektuell.bass.app"
    mainClass = "de.infolektuell.bass.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.bass.main")
}
