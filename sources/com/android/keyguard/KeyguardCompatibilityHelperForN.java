package com.android.keyguard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.security.MiuiLockPatternUtils;
import android.util.Log;

public class KeyguardCompatibilityHelperForN {
    private static boolean sIsUserUnlocked = false;

    public static String getOwnerInfo(MiuiLockPatternUtils lockPatternUtils, int userId) {
        if (lockPatternUtils.isDeviceOwnerInfoEnabled()) {
            return lockPatternUtils.getDeviceOwnerInfo();
        }
        if (lockPatternUtils.isOwnerInfoEnabled(userId)) {
            return lockPatternUtils.getOwnerInfo(userId);
        }
        return null;
    }

    public static boolean isUserUnlocked(Context context) {
        if (!sIsUserUnlocked) {
            sIsUserUnlocked = ((UserManager) context.getSystemService(UserManager.class)).isUserUnlocked();
        }
        return sIsUserUnlocked;
    }

    public static boolean isAppInstalledForUser(Context context, String pkg, int userId) {
        try {
            context.getPackageManager().getPackageInfoAsUser(pkg, 1, userId);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("miui_keyguard", "name not found pkg=" + pkg);
            return false;
        }
    }
}
