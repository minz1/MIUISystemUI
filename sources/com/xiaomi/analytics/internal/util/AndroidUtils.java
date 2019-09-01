package com.xiaomi.analytics.internal.util;

import android.content.Context;
import android.content.pm.Signature;

public class AndroidUtils {
    public static Signature[] getSignature(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 64).signatures;
        } catch (Exception e) {
            return null;
        }
    }
}
