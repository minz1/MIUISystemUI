package com.android.systemui;

import android.content.ContentResolver;
import android.provider.Settings;
import com.android.keyguard.KeyguardUpdateMonitor;

public class UserSettingsUtils {
    public static void save(ContentResolver cr, boolean enabled) {
        Settings.Secure.putIntForUser(cr, "systemui.google.opa_enabled", (int) enabled, KeyguardUpdateMonitor.getCurrentUser());
    }

    public static boolean load(ContentResolver cr) {
        if (Settings.Secure.getIntForUser(cr, "systemui.google.opa_enabled", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0) {
            return true;
        }
        return false;
    }
}
