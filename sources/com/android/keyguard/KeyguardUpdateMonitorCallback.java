package com.android.keyguard;

import android.os.SystemClock;
import android.telephony.ServiceState;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;

public class KeyguardUpdateMonitorCallback {
    private boolean mShowing;
    private long mVisibilityChangedCalled;

    public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
    }

    public void onTimeChanged() {
    }

    public void onRefreshCarrierInfo() {
    }

    public void onRingerModeChanged(int state) {
    }

    public void onPhoneStateChanged(int phoneState) {
    }

    public void onKeyguardVisibilityChanged(boolean showing) {
    }

    public void onKeyguardVisibilityChangedRaw(boolean showing) {
        long now = SystemClock.elapsedRealtime();
        if (showing != this.mShowing || now - this.mVisibilityChangedCalled >= 1000) {
            onKeyguardVisibilityChanged(showing);
            this.mVisibilityChangedCalled = now;
            this.mShowing = showing;
        }
    }

    public void onKeyguardBouncerChanged(boolean bouncer) {
    }

    public void onClockVisibilityChanged() {
    }

    public void onDeviceProvisioned() {
    }

    public void onDevicePolicyManagerStateChanged() {
    }

    public void onUserSwitching(int userId) {
    }

    public void onUserSwitchComplete(int userId) {
    }

    public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
    }

    public void onServiceStateChanged(int subId, ServiceState state) {
    }

    public void onAirplaneModeChanged() {
    }

    public void onUserInfoChanged(int userId) {
    }

    public void onUserUnlocked() {
    }

    public void onBootCompleted() {
    }

    public void onEmergencyCallAction() {
    }

    public void onStartedWakingUp() {
    }

    public void onStartedGoingToSleep(int why) {
    }

    public void onFinishedGoingToSleep(int why) {
    }

    public void onScreenTurnedOn() {
    }

    public void onScreenTurnedOff() {
    }

    public void onFingerprintAcquired() {
    }

    public void onFingerprintAuthFailed() {
    }

    public void onFingerprintAuthenticated(int userId) {
    }

    public void onFingerprintHelp(int msgId, String helpString) {
    }

    public void onFingerprintError(int msgId, String errString) {
    }

    public void onFaceUnlockStateChanged(boolean running, int userId) {
    }

    public void onFingerprintRunningStateChanged(boolean running) {
    }

    public void onFingerprintLockoutReset() {
    }

    public void onStrongAuthStateChanged(int userId) {
    }

    public void onHasLockscreenWallpaperChanged(boolean hasLockscreenWallpaper) {
    }

    public void onDreamingStateChanged(boolean dreaming) {
    }

    public void onFaceHelp(int helpStringId) {
    }

    public void onFaceAuthFailed(boolean hasFace) {
    }

    public void onFaceAuthenticated() {
    }

    public void onFaceStart() {
    }

    public void onFaceStop() {
    }

    public void onFaceLocked() {
    }

    public void unblockScreenOn() {
    }

    public void onRegionChanged() {
    }

    public void updateShowingStatus(boolean showing) {
    }

    public void onLockScreenMagazineStatusChanged() {
    }

    public void onBottomAreaButtonClicked(boolean isClickAnimating) {
    }

    public void restartFaceUnlock() {
    }

    public void onLockScreenMagazinePreViewVisibilityChanged(boolean visible) {
    }

    public void onPhoneSignalChanged(boolean isSignalAvailable) {
    }
}
