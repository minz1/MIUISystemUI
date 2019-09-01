package android.app.admin;

import android.content.ComponentName;

public class DevicePolicyManagerCompat {
    public static CharSequence getDeviceOwnerOrganizationName(DevicePolicyManager devicePolicyManager) {
        return devicePolicyManager.getDeviceOwnerOrganizationName();
    }

    public static boolean isNetworkLoggingEnabled(DevicePolicyManager devicePolicyManager) {
        return devicePolicyManager.isNetworkLoggingEnabled(null);
    }

    public static CharSequence getOrganizationNameForUser(DevicePolicyManager dpm, int userHandle) {
        return dpm.getOrganizationNameForUser(userHandle);
    }

    public static boolean isDeviceManaged(DevicePolicyManager dpm) {
        return dpm.isDeviceManaged();
    }

    public static void reportFailedFingerprintAttempt(DevicePolicyManager dpm, int userHandle) {
        dpm.reportFailedFingerprintAttempt(userHandle);
    }

    public static void reportSuccessfulFingerprintAttempt(DevicePolicyManager dpm, int userHandle) {
        dpm.reportSuccessfulFingerprintAttempt(userHandle);
    }

    public static void reportKeyguardDismissed(DevicePolicyManager dpm, int userHandle) {
        dpm.reportKeyguardDismissed(userHandle);
    }

    public static void reportKeyguardSecured(DevicePolicyManager dpm, int userHandle) {
        dpm.reportKeyguardSecured(userHandle);
    }

    public static long getRequiredStrongAuthTimeout(DevicePolicyManager dpm, ComponentName admin, int userId) {
        return dpm.getRequiredStrongAuthTimeout(admin, userId);
    }
}
