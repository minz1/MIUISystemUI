package com.xiaomi.mistatistic.sdk;

import android.util.Log;
import com.xiaomi.mistatistic.sdk.controller.c;
import com.xiaomi.mistatistic.sdk.controller.q;

public class CustomSettings {
    private static boolean a = false;
    private static boolean b = false;
    private static boolean c = true;
    private static boolean d = false;
    private static boolean e = false;

    public static boolean isUseSystemUploadingService() {
        return a;
    }

    public static void setUseSystemStatService(boolean z) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
        } else if (!z || (c.a().getApplicationInfo().flags & 1) != 0) {
            b = z;
        }
    }

    public static boolean isUseSystemStatService() {
        return b;
    }

    public static boolean isDataUploadingEnabled() {
        return c;
    }

    public static boolean isUploadInstalledPackageEnabled() {
        return d;
    }

    public static boolean isUploadForegroundPackageEnabled() {
        return e;
    }
}
