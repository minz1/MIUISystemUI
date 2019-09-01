package com.android.internal.widget;

import com.android.internal.widget.LockPatternUtils;

public class LockPatternUtilsCompat {
    public static void reportPasswordLockout(LockPatternUtils lockPatternUtils, int timeoutMs, int userId) {
        lockPatternUtils.reportPasswordLockout(timeoutMs, userId);
    }

    public static boolean isSeparateProfileChallengeEnabled(LockPatternUtils lockPatternUtils, int userHandle) {
        return lockPatternUtils.isSeparateProfileChallengeEnabled(userHandle);
    }

    public static void userPresent(LockPatternUtils lockPatternUtils, int userId) {
        lockPatternUtils.userPresent(userId);
    }

    public static String getDeviceOwnerInfo(LockPatternUtils lockPatternUtils) {
        return lockPatternUtils.getDeviceOwnerInfo();
    }

    public static boolean isDeviceOwnerInfoEnabled(LockPatternUtils lockPatternUtils) {
        return lockPatternUtils.isDeviceOwnerInfoEnabled();
    }

    public static void registerStrongAuthTracker(LockPatternUtils lockPatternUtils, LockPatternUtils.StrongAuthTracker strongAuthTracker) {
        lockPatternUtils.registerStrongAuthTracker(strongAuthTracker);
    }

    public static void requireStrongAuth(LockPatternUtils lockPatternUtils, int strongAuthReason, int userId) {
        lockPatternUtils.requireStrongAuth(strongAuthReason, userId);
    }
}
