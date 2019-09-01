package com.android.systemui;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

public final class Prefs {
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return get(context).getBoolean(key, defaultValue);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        get(context).edit().putBoolean(key, value).apply();
    }

    public static Map<String, ?> getAll(Context context) {
        return get(context).getAll();
    }

    private static SharedPreferences get(Context context) {
        return context.getSharedPreferences(context.getPackageName(), 0);
    }
}
