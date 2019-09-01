package com.xiaomi.analytics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import com.xiaomi.analytics.LogEvent;
import com.xiaomi.analytics.internal.util.ALog;
import com.xiaomi.analytics.internal.util.AndroidUtils;
import com.xiaomi.analytics.internal.util.CertificateUtils;

public class Analytics {
    private static volatile boolean sUpdateEnable = true;

    public static void trackSystem(Context context, String key, Action action) throws Exception {
        if (isSystemPackage(context) || isPlatformSignature(context)) {
            Intent intent = new Intent();
            intent.setClassName("com.miui.analytics", "com.miui.analytics.EventService");
            intent.putExtra("key", key != null ? key : "");
            intent.putExtra("content", action.getContent().toString());
            intent.putExtra("extra", action.getExtra().toString());
            if (context.getApplicationContext() != null) {
                intent.putExtra("appid", context.getPackageName());
            }
            if (action instanceof AdAction) {
                intent.putExtra("type", LogEvent.LogType.TYPE_AD.value());
            } else {
                intent.putExtra("type", LogEvent.LogType.TYPE_EVENT.value());
            }
            context.startService(intent);
            return;
        }
        throw new IllegalArgumentException("App is not allowed to use this method to track event, except system or platform signed apps. Use getTracker instead.");
    }

    private static boolean isPlatformSignature(Context context) {
        boolean ret = CertificateUtils.isXiaomiPlatformCertificate(AndroidUtils.getSignature(context, context.getPackageName()));
        Log.d(ALog.addPrefix("Analytics"), String.format("%s is platform signatures : %b", new Object[]{context.getPackageName(), Boolean.valueOf(ret)}));
        return ret;
    }

    private static boolean isSystemPackage(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        return (info == null || (info.flags & 1) == 0) ? false : true;
    }
}
