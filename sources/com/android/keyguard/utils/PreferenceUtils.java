package com.android.keyguard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.miui.systemui.support.v4.content.ContextCompat;

public class PreferenceUtils {
    public static void putBoolean(Context context, String key, boolean value) {
        getSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    public static void putString(Context context, String key, String value) {
        getSharedPreferences(context).edit().putString(key, value).apply();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return getContext(context).getSharedPreferences("keyguard_sharedpreference", 0);
    }

    private static Context getContext(Context context) {
        Context storageContext = ContextCompat.createDeviceProtectedStorageContext(context);
        if (storageContext != null) {
            return storageContext;
        }
        return context;
    }

    public static void removeKey(Context context, String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences.contains(key)) {
            sharedPreferences.edit().remove(key).apply();
        }
    }
}
