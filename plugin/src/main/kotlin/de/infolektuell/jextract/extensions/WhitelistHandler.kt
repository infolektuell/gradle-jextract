package de.infolektuell.jextract.extensions

import org.gradle.api.provider.ListProperty

abstract class WhitelistHandler {
    abstract val functions: ListProperty<String>
    abstract val constants: ListProperty<String>
    abstract val structs: ListProperty<String>
    abstract val typedefs: ListProperty<String>
    abstract val unions: ListProperty<String>
    abstract val variables: ListProperty<String>
}
