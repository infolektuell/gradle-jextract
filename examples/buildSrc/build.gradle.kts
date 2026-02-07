plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("de.infolektuell:gradle-plugin-jextract")
}
