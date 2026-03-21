pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "snapshot"
            url = uri("https://nexus.infolektuell.de/repository/maven-snapshots/")
        }
    }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "jextract-example-project"
include("app")
include("lib")
include("nativelib")
includeBuild("../")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
