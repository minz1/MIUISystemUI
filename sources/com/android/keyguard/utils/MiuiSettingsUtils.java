package com.android.keyguard.utils;

import android.content.ContentResolver;
import android.provider.MiuiSettings;

public class MiuiSettingsUtils {
    public static boolean putStringToSystem(ContentResolver resolver, String name, String value) {
        return MiuiSettings.System.putString(resolver, name, value);
    }
}
