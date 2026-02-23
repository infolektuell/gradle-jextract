package de.infolektuell.bass;

import com.un4seen.bass.Bass;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

final class LibLoader {
    private static boolean loaded = false;

    static synchronized void loadLibraries() {
        if (loaded) return;
        final String libName = System.mapLibraryName("bass");
        try (InputStream s = Bass.class.getResourceAsStream("/" + libName)) {
            if (Objects.isNull(s)) return;
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
