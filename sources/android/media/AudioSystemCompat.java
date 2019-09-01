package android.media;

import android.content.Context;

public class AudioSystemCompat {
    public static boolean isSingleVolume(Context context) {
        return AudioSystem.isSingleVolume(context);
    }
}
