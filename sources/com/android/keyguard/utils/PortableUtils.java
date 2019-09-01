package com.android.keyguard.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.preference.PreferenceManager;

public class PortableUtils {
    public static String getCurrentWallpaperInfo(Context context, String contentUri) {
        if (Build.VERSION.SDK_INT < 21) {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("currentWallpaperInfo", "");
        }
        Cursor cursor = null;
        try {
            Cursor cursor2 = context.createPackageContextAsUser(context.getPackageName(), 2, UserHandle.OWNER).getContentResolverForUser(UserHandle.OWNER).query(Uri.parse(contentUri), new String[]{"currentWallpaperInfo"}, null, null, null);
            if (cursor2 != null) {
                cursor2.moveToFirst();
                String string = cursor2.getString(0);
                if (cursor2 != null) {
                    cursor2.close();
                }
                return string;
            }
            if (cursor2 != null) {
                cursor2.close();
            }
            return null;
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
            return null;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    public static void updateCurrentWallpaperInfo(Context context, String info, String contentUri) {
        if (Build.VERSION.SDK_INT < 21) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("currentWallpaperInfo", info).commit();
            return;
        }
        try {
            ContentResolver contentResolver = context.createPackageContextAsUser(context.getPackageName(), 2, UserHandle.OWNER).getContentResolverForUser(UserHandle.OWNER);
            ContentValues values = new ContentValues();
            values.put("currentWallpaperInfo", info);
            contentResolver.update(Uri.parse(contentUri), values, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
