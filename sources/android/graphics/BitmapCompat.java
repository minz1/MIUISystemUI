package android.graphics;

import android.graphics.Bitmap;

public class BitmapCompat {
    public static boolean isConfigHardware(Bitmap bitmap) {
        return bitmap.getConfig() == Bitmap.Config.HARDWARE;
    }
}
