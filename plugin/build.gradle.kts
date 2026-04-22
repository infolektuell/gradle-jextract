plugins {
    signing
    id("com.gradle.plugin-publish") version "2.0.0"
}

val releaseVersion = releaseVersion().get()
val releaseNotes = releaseNotes().get()
version = releaseVersion

gradlePlugin {
    website = "https://infolektuell.github.io/gradle-jextract/"
    vcsUrl = "https://github.com/infolektuell/gradle-jextract.git"
    plugins.create("jextractPlugin") {
        id = "de.infolektuell.jextract"
        displayName = "jextract gradle plugin"
        description = releaseNotes
        tags = listOf("native", "FFM", "panama", "jextract")
        implementationClass = "de.infolektuell.gradle.jextract.GradleJextractPlugin"
    }
}

publishing {
    repositories {
        maven {
            name = "release"
            url = uri("https://nexus.infolektuell.de/repository/maven-releases/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "snapshot"
            url = uri("https://nexus.infolektuell.de/repository/maven-snapshots/")
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    // Get credentials from env variables for better CI compatibility
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
}

tasks.withType<Sign>().configureEach {
    val isReleaseVersion = !releaseVersion.endsWith("-SNAPSHOT")
    onlyIf { isReleaseVersion }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(Javadoc::class).named("javadoc") {
    javadocTool = javaToolchains.javadocToolFor { languageVersion = JavaLanguageVersion.of(25) }
    options.destinationDirectory = rootProject.layout.projectDirectory.dir("docs/public/reference").asFile
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")

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

fun releaseVersion(): Provider<String> {
    val releaseVersionFile = rootProject.layout.projectDirectory.file("release/version.txt")
    return providers.fileContents(releaseVersionFile).asText.map(String::trim)
}

fun releaseNotes(): Provider<String> {
    val releaseNotesFile = rootProject.layout.projectDirectory.file("release/changes.md")
    return providers.fileContents(releaseNotesFile).asText.map(String::trim)
}
