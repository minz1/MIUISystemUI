package com.android.keyguard;

import android.os.Build;

public class DeviceConfig {
    public static int TEMP_SHARE_MODE_FOR_WORLD_READABLE = (Build.VERSION.SDK_INT >= 24 ? 0 : 1);
}
