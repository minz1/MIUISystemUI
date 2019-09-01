package com.android.keyguard.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.android.keyguard.common.BackgroundThread;
import miui.content.res.ThemeNativeUtils;

public class ThemeUtils {
    public static void tellThemeLockWallpaperPath(final Context context, final String path) {
        BackgroundThread.post(new Runnable() {
            public void run() {
                Uri tableUri = Uri.parse("content://com.android.thememanager.provider/lockscreen");
                ContentValues values = new ContentValues();
                values.put("key_lockscreen_path", path);
                ContentProviderUtils.updateData(context, tableUri, values);
            }
        });
    }

    public static boolean updateFilePermissionWithThemeContext(String path) {
        return ThemeNativeUtils.updateFilePermissionWithThemeContext(path);
    }
}
