plugins {
    id("app-conventions")
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainModule = "de.infolektuell.bass.app"
    mainClass = "de.infolektuell.bass.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.bass.main")
}
