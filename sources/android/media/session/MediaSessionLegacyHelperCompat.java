package android.media.session;

import android.content.Context;
import android.view.KeyEvent;

public class MediaSessionLegacyHelperCompat {
    public static void sendVolumeKeyEvent(Context context, KeyEvent event, int stream, boolean musicOnly) {
        MediaSessionLegacyHelper.getHelper(context).sendVolumeKeyEvent(event, stream, musicOnly);
    }
}
