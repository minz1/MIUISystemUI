package com.android.systemui.miui.statusbar.phone.applock;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.Constants;
import miui.security.SecurityManager;
import miui.securityspace.XSpaceUserHandle;

public class AppLockHelper {
    private static final boolean DEBUG = Constants.DEBUG;

    public static boolean shouldShowPublicNotificationByAppLock(Context context, SecurityManager sm, String pkg, int userId) {
        boolean lockOn = isACLockEnabledAsUser(context.getContentResolver(), userId) && getApplicationAccessControlEnabledAsUser(sm, pkg, userId) && getApplicationMaskNotificationEnabledAsUser(sm, pkg, userId) && !checkAccessControlPassAsUser(sm, pkg, userId);
        if (DEBUG) {
            Log.d("AppLockHelper", "shouldShowPublicNotificationByAppLock() lockOn=" + lockOn + "; pkg=" + pkg + "; userId=" + userId);
        }
        return lockOn;
    }

    public static boolean isAppLocked(Context context, SecurityManager sm, String pkg, int userId) {
        boolean lockOn = isACLockEnabledAsUser(context.getContentResolver(), userId) && getApplicationAccessControlEnabledAsUser(sm, pkg, userId) && !checkAccessControlPassAsUser(sm, pkg, userId);
        if (DEBUG) {
            Log.d("AppLockHelper", "isAppLocked() lockOn=" + lockOn + "; pkg=" + pkg + "; userId=" + userId);
        }
        return lockOn;
    }

    public static int getCurrentUserIdIfNeeded(int originalUserId, int currentUserId) {
        if (DEBUG) {
            Log.d("AppLockHelper", "getCurrentUserIdIfNeeded() originalUserId=" + originalUserId + "; currentUserId=" + currentUserId);
        }
        if (currentUserId < 0) {
            Log.e("AppLockHelper", "getCurrentUserIdIfNeeded() error currentUserId < 0: originalUserId=" + originalUserId + "; currentUserId=" + currentUserId);
            currentUserId = 0;
        }
        if (originalUserId < 0) {
            return currentUserId;
        }
        return originalUserId;
    }

    private static boolean isACLockEnabledAsUser(ContentResolver resolver, int userId) {
        boolean z = true;
        if (Settings.Secure.getIntForUser(resolver, "access_control_lock_enabled", -1, getUserIdIgnoreXspace(userId)) != 1) {
            z = false;
        }
        boolean lockOn = z;
        if (DEBUG) {
            Log.d("AppLockHelper", "isACLockEnabledAsUser() lockOn=" + lockOn);
        }
        return lockOn;
    }

    private static boolean getApplicationMaskNotificationEnabledAsUser(SecurityManager sm, String pkg, int userId) {
        boolean isMasked = sm.getApplicationMaskNotificationEnabledAsUser(pkg, userId);
        if (DEBUG) {
            Log.d("AppLockHelper", "getApplicationMaskNotificationEnabledAsUser() lockOn=" + isMasked);
        }
        return isMasked;
    }

    private static boolean checkAccessControlPassAsUser(SecurityManager sm, String pkg, int userId) {
        boolean isMasked = sm.checkAccessControlPassAsUser(pkg, userId);
        if (DEBUG) {
            Log.d("AppLockHelper", "checkAccessControlPassAsUser() isMasked=" + isMasked);
        }
        return isMasked;
    }

    private static boolean getApplicationAccessControlEnabledAsUser(SecurityManager sm, String pkg, int userId) {
        boolean lockOn = sm.getApplicationAccessControlEnabledAsUser(pkg, userId);
        if (DEBUG) {
            Log.d("AppLockHelper", "getApplicationAccessControlEnabledAsUser() lockOn=" + lockOn);
        }
        return lockOn;
    }

    private static int getUserIdIgnoreXspace(int userId) {
        if (XSpaceUserHandle.isXSpaceUserId(userId)) {
            return 0;
        }
        return userId;
    }
}
