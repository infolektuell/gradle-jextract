plugins {
    // Apply the cpp-library plugin to add support for building C++ libraries
    `cpp-library`

    // Apply the cpp-unit-test plugin to add support for building and running C++ test executables
    `cpp-unit-test`
}

tasks.withType(CppCompile::class) {
    doLast {
        println("Finished native compilation task $name")
    }
}
