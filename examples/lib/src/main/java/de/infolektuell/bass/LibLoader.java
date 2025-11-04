package de.infolektuell.bass;

import com.un4seen.bass.Bass;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LibLoader {
    boolean loaded = false;
    void loadLibraries() {
        if (loaded) {
            return;
        }
        final String libName = System.mapLibraryName("bass");
    String osName;
    if (libName.endsWith(".dylib")) {
        osName = "macos";
    } else if(libName.endsWith(".dll")) {
        osName = "windows";
    } else  {
        osName = "linux";
    }
    final String archName = "x64";
    InputStream s = Bass.class.getResourceAsStream("/native/" + osName + "/" + archName + "/" + libName);
    if (s == null) {
        return;
    }
    try {
        Path tmpDir = Files.createTempDirectory("bass");
        tmpDir.toFile().deleteOnExit();
        Path file = tmpDir.resolve(libName);
        Files.copy(s, file, StandardCopyOption.REPLACE_EXISTING);
        System.load(file.toString());
        loaded = true;
    } catch (Exception e) {
        loaded = false;
    }
}
}
