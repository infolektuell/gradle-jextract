package de.infolektuell.bass;

import java.nio.ByteBuffer;

public final class Bass {
    LibLoader loader = new LibLoader();
    public Bass() {
        loader.loadLibraries();
    }

    public VersionNumber getVersion() {
        int version = com.un4seen.bass.Bass.BASS_GetVersion();
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(version);
        byte[] bytes = buf.array();
        int[] ints = new int[4];
        for (int i = 0; i < ints.length; i++){
            ints[i] = bytes[i];
        }
        return new VersionNumber(ints[0], ints[1], ints[2], ints[3]);
    }
}
