package de.infolektuell.bass

import com.un4seen.bass.Bass
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object LibLoader {
    private var loaded = false
    fun loadLibraries() {
        if (loaded) return
        val libName = System.mapLibraryName("bass")
        val osName = if (libName.endsWith(".dylib")) {
            "macos"
        } else if(libName.endsWith(".dll")) {
            "windows"
        } else  {
            "linux"
        }
        val archName = "x64"
        Bass::class.java.getResourceAsStream("/native/${osName}/${archName}/${libName}")?.let { s ->
            val tmpDir = Files.createTempDirectory("bass")
            val file = tmpDir.resolve(libName)
            Files.copy(s, file, StandardCopyOption.REPLACE_EXISTING)
            System.load(file.toString())
            loaded = true
            Files.delete(file)
        }
    }
}
