plugins {
    kotlin("jvm") version "2.1.21"
    signing
    id("com.gradle.plugin-publish") version "1.3.1"
}

gradlePlugin {
    website = "https://github.com/infolektuell/gradle-jextract"
    vcsUrl = "https://github.com/infolektuell/gradle-jextract.git"
    plugins.create("jextractPlugin") {
        id = "de.infolektuell.jextract"
        displayName = "jextract gradle plugin"
        description = "Generates Java bindings from native library headers using the Jextract tool"
        tags = listOf("native", "FFM", "panama", "jextract")
        implementationClass = "de.infolektuell.gradle.jextract.GradleJextractPlugin"
    }
}

signing {
    // Get credentials from env variables for better CI compatibility
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
