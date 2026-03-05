plugins {
    // Apply the cpp-library plugin to add support for building C++ libraries
    `cpp-library`

    // Apply the cpp-unit-test plugin to add support for building and running C++ test executables
    `cpp-unit-test`
}

library {
    // Set the target operating system and architecture for this library
    targetMachines.add(machines.macOS.architecture("aarch64"))
}
