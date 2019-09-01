package com.android.keyguard.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import com.android.keyguard.KeyguardCompatibilityHelperForN;
import com.android.keyguard.KeyguardUpdateMonitor;

public class PackageUtils {
    public static boolean isAppInstalledForUser(Context context, String pkg, int userId) {
        return KeyguardCompatibilityHelperForN.isAppInstalledForUser(context, pkg, userId);
    }

    public static Drawable getDrawableFromPackage(Context context, String pkgName, String name) {
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(pkgName);
            return resources.getDrawable(resources.getIdentifier(name, "drawable", pkgName));
        } catch (Exception e) {
            Log.e("miui_keyguard", "something wrong when get image");
            e.printStackTrace();
            return null;
        }
    }

    public static ResolveInfo resolveIntent(Context context, Intent intent) {
        return resolveIntent(context, intent, 0);
    }

    public static ResolveInfo resolveIntent(Context context, Intent intent, int flags) {
        if (intent == null) {
            return null;
        }
        try {
            return context.getPackageManager().resolveActivityAsUser(intent, flags, KeyguardUpdateMonitor.getCurrentUser());
        } catch (IllegalStateException e) {
            Log.e("PackageUtils", "resolveIntent exception", e);
            return null;
        }
    }

    public static Intent getTSMClientIntent() {
        Intent intent = new Intent();
        intent.setAction("com.miui.intent.action.DOUBLE_CLICK");
        intent.putExtra("event_source", "shortcut_of_all_cards");
        intent.addFlags(268435456);
        return intent;
    }

    public static Intent getToggleTorchIntent(boolean enable) {
        Intent intent = new Intent("miui.intent.action.TOGGLE_TORCH");
        intent.putExtra("miui.intent.extra.IS_ENABLE", enable);
        return intent;
    }

    public static Intent getSmartHomeMainIntent() {
        Intent intent = new Intent();
        intent.setPackage("com.xiaomi.smarthome");
        intent.setData(Uri.parse("http://home.mi.com/main"));
        intent.putExtra("source", 11);
        intent.setAction("android.intent.action.VIEW");
        intent.addFlags(268435456);
        return intent;
    }

    public static Intent getMarketDownloadIntent(String packageName) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("market://details?id=" + packageName + "&back=true&ref=keyguard"));
        intent.setAction("android.intent.action.VIEW");
        intent.addFlags(268435456);
        return intent;
    }
}
