package de.infolektuell.bass

import com.un4seen.bass.Bass

object LibLoader {
    private var loaded = false
    fun loadLibraries() {
        if (loaded) return
        val libName = System.mapLibraryName("bass")
        Bass::class.java.getResource("/native/mac/${libName}")?.let { s ->
            System.load(s.file)
            loaded = true
        }
    }
}
