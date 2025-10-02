plugins {
    id("common-conventions")
    application
}

dependencies {
    implementation(project(":lib"))
}

// Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
tasks.named("compileJava", JavaCompile::class.java) {
    sourceSets.main {
        options.compilerArgs.addAll(listOf("--patch-module", "de.infolektuell.bass.app=${output.asPath}"))
    }
}

application {
    mainClass = "de.infolektuell.bass.app.MainKt"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
