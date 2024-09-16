package de.infolektuell.gradle.jextract.extensions

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class WhitelistHandler @Inject constructor(providers: ProviderFactory) {
    abstract val functions: ListProperty<String>
    abstract val constants: ListProperty<String>
    abstract val structs: ListProperty<String>
    abstract val typedefs: ListProperty<String>
    abstract val unions: ListProperty<String>
    abstract val variables: ListProperty<String>
    val mapProvider: Provider<Map<String, List<String>>> = providers.provider {
        mapOf(
            "function" to functions.get(),
            "constant" to constants.get(),
            "struct" to structs.get(),
            "typedef" to typedefs.get(),
            "union" to unions.get(),
            "var" to variables.get(),
        )
    }
}
