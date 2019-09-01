package com.android.keyguard.utils;

import android.os.SystemProperties;

public class HomeUtils {
    public static final String HOME_LAUNCHER_SETTINGS_AUTHORITY = (SystemProperties.get("ro.miui.product.home", "com.miui.home") + ".launcher.settings");
}
