package com.android.keyguard.widget;

import android.os.Build;

public class AODKeys {
    public static final String AOD_MODE = (Build.VERSION.SDK_INT >= 28 ? "doze_always_on" : "aod_mode");
}
