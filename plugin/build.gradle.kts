plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    val jextract by plugins.creating {
        id = "de.infolektuell.jextract"
        displayName = "jextract gradle plugin"
        description = "Generates Java bindings from native library headers using the Jextract tool"
        implementationClass = "de.infolektuell.jextract.GradleJextractPlugin"
        website = "https://github.com/infolektuell/gradle-jextract"
        vcsUrl = "https://github.com/infolektuell/gradle-jextract.git"
        tags = listOf("native", "panama", "jextract")
    }
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
