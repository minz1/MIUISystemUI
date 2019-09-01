package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.MiuiSettings;
import android.util.Log;
import android.util.Slog;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.LatencyTracker;
import com.android.keyguard.MiuiKeyguardFingerprintUtils;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.KeyguardViewMediator;

public class FingerprintUnlockController extends KeyguardUpdateMonitorCallback {
    private boolean mCancelingPendingLock = false;
    private final Context mContext;
    private DozeScrimController mDozeScrimController;
    private MiuiKeyguardFingerprintUtils.FingerprintIdentificationState mFpiState;
    private Handler mHandler = new Handler();
    private KeyguardViewMediator mKeyguardViewMediator;
    private int mMode;
    private int mPendingAuthenticatedUserId = -1;
    private PowerManager mPowerManager;
    private final Runnable mReleaseFingerprintWakeLockRunnable = new Runnable() {
        public void run() {
            Log.i("FingerprintController", "fp wakelock: TIMEOUT!!");
            FingerprintUnlockController.this.releaseFingerprintWakeLock();
        }
    };
    private ScrimController mScrimController;
    private StatusBar mStatusBar;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    /* access modifiers changed from: private */
    public StatusBarWindowManager mStatusBarWindowManager;
    private final UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private PowerManager.WakeLock mWakeLock;

    public FingerprintUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this);
        this.mStatusBarWindowManager = (StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class);
        this.mDozeScrimController = dozeScrimController;
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mScrimController = scrimController;
        this.mStatusBar = statusBar;
        this.mUnlockMethodCache = unlockMethodCache;
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    /* access modifiers changed from: private */
    public void releaseFingerprintWakeLock() {
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mReleaseFingerprintWakeLockRunnable);
            Log.i("FingerprintController", "releasing fp wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    public void onFingerprintAcquired() {
        Trace.beginSection("FingerprintUnlockController#onFingerprintAcquired");
        releaseFingerprintWakeLock();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (LatencyTracker.isEnabled(this.mContext)) {
                LatencyTracker.getInstance(this.mContext).onActionStart(2);
            }
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
            Trace.beginSection("acquiring wake-and-unlock");
            this.mWakeLock.acquire();
            Trace.endSection();
            Log.i("FingerprintController", "fingerprint acquired, grabbing fp wakelock");
            this.mHandler.postDelayed(this.mReleaseFingerprintWakeLockRunnable, 15000);
            if (this.mDozeScrimController.isPulsing()) {
                this.mStatusBarWindowManager.setForceDozeBrightness(true);
            }
        }
        Trace.endSection();
    }

    public void onFingerprintAuthenticated(int userId) {
        Trace.beginSection("FingerprintUnlockController#onFingerprintAuthenticated");
        if (this.mKeyguardViewMediator.isGoingToSleep()) {
            int mode = 0;
            boolean unlockingAllowed = this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(userId);
            if (this.mDozeScrimController.isPulsing() && unlockingAllowed) {
                mode = 2;
            } else if (unlockingAllowed || !this.mUnlockMethodCache.isMethodSecure()) {
                mode = 1;
            }
            this.mUpdateMonitor.setFingerprintMode(mode);
            if (this.mKeyguardViewMediator.isShowing() || KeyguardUpdateMonitor.getCurrentUser() != userId || (!(mode == 2 || mode == 1) || !MiuiKeyguardUtils.isAodClockDisable(this.mContext))) {
                this.mPendingAuthenticatedUserId = userId;
                this.mKeyguardViewMediator.recordFingerprintUnlockState();
            } else {
                Slog.i("miui_keyguard_fingerprint", "Unlock by fingerprint, keyguard is not showing and wake up");
                recordUnlockWay();
                this.mKeyguardViewMediator.cancelPendingLock();
                synchronized (this) {
                    this.mCancelingPendingLock = true;
                }
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:FINGERPRINT");
            }
            Trace.endSection();
            return;
        }
        boolean wasDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        this.mMode = calculateMode(userId);
        if (!(KeyguardUpdateMonitor.getCurrentUser() == userId || this.mMode == 3 || this.mMode == 0 || this.mMode == 4)) {
            if (MiuiKeyguardUtils.canSwitchUser(this.mContext, userId)) {
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    MiuiGxzwManager.getInstance().onKeyguardHide();
                }
                try {
                    ActivityManagerNative.getDefault().switchUser(userId);
                } catch (RemoteException e) {
                    Log.e("FingerprintController", "switchUser failed", e);
                }
            } else {
                this.mMode = 3;
            }
        }
        this.mUpdateMonitor.setFingerprintMode(this.mMode);
        Class<?> cls = getClass();
        PanelBar.LOG((Class) cls, "calculateMode userid=" + userId + ";mode=" + this.mMode);
        if (!wasDeviceInteractive) {
            Log.i("FingerprintController", "fp wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:FINGERPRINT");
        }
        Trace.beginSection("release wake-and-unlock");
        releaseFingerprintWakeLock();
        Trace.endSection();
        switch (this.mMode) {
            case 1:
            case 2:
                if (this.mMode == 2) {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK_PULSING");
                    this.mStatusBar.updateMediaMetaData(false, true);
                } else {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK");
                    this.mDozeScrimController.abortDoze();
                }
                keyguardDoneWithoutHomeAnim();
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mKeyguardViewMediator.onWakeAndUnlocking();
                this.mScrimController.setWakeAndUnlocking();
                this.mDozeScrimController.setWakeAndUnlocking();
                if (this.mStatusBar.getNavigationBarView() != null) {
                    this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(true);
                }
                recordUnlockWay();
                Trace.endSection();
                break;
            case 3:
                Trace.beginSection("MODE_SHOW_BOUNCER");
                if (!wasDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
                Trace.endSection();
                break;
            case 5:
                Trace.beginSection("MODE_UNLOCK");
                keyguardDoneWithoutHomeAnim();
                if (!wasDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mKeyguardViewMediator.keyguardDone();
                recordUnlockWay();
                Trace.endSection();
                break;
            case 6:
                Trace.beginSection("MODE_DISMISS");
                this.mStatusBarKeyguardViewManager.notifyKeyguardAuthenticated(false);
                recordUnlockWay();
                Trace.endSection();
                break;
        }
        if (this.mMode != 2) {
            this.mStatusBarWindowManager.setForceDozeBrightness(false);
        }
        this.mStatusBar.notifyFpAuthModeChanged();
        this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.SUCCEEDED;
        MiuiKeyguardFingerprintUtils.processFingerprintResultAnalytics(1);
        Trace.endSection();
    }

    private void keyguardDoneWithoutHomeAnim() {
        MiuiSettings.System.putBooleanForUser(this.mContext.getContentResolver(), "is_fingerprint_unlock", true, -2);
    }

    public void onStartedGoingToSleep(int why) {
        this.mPendingAuthenticatedUserId = -1;
    }

    public void onFinishedGoingToSleep(int why) {
        Trace.beginSection("FingerprintUnlockController#onFinishedGoingToSleep");
        if (this.mPendingAuthenticatedUserId != -1) {
            final int pendingUserId = this.mPendingAuthenticatedUserId;
            this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintUnlockController.this.onFingerprintAuthenticated(pendingUserId);
                }
            });
        }
        this.mPendingAuthenticatedUserId = -1;
        Trace.endSection();
    }

    public int getMode() {
        return this.mMode;
    }

    private int calculateMode(int userId) {
        boolean unlockingAllowed = this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(userId);
        if (this.mUpdateMonitor.isDeviceInteractive()) {
            if (this.mStatusBarKeyguardViewManager.isShowing()) {
                if (this.mStatusBarKeyguardViewManager.isBouncerShowing() && unlockingAllowed) {
                    return 6;
                }
                if (unlockingAllowed) {
                    return 5;
                }
                if (!this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    return 3;
                }
            }
            return 0;
        } else if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            return 4;
        } else {
            if (this.mDozeScrimController.isPulsing() && unlockingAllowed) {
                return 2;
            }
            if (unlockingAllowed || !this.mUnlockMethodCache.isMethodSecure()) {
                return 1;
            }
            return 3;
        }
    }

    public void onFingerprintAuthFailed() {
        cleanup();
        this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.FAILED;
        MiuiKeyguardFingerprintUtils.processFingerprintResultAnalytics(0);
    }

    public void onFingerprintError(int msgId, String errString) {
        cleanup();
        if (MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR != this.mFpiState && (msgId == 7 || msgId == 9)) {
            this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
        }
        this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR;
    }

    private void cleanup() {
        releaseFingerprintWakeLock();
    }

    public void startKeyguardFadingAway() {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                FingerprintUnlockController.this.mStatusBarWindowManager.setForceDozeBrightness(false);
            }
        }, 96);
    }

    public void finishKeyguardFadingAway() {
        resetMode();
        this.mStatusBarWindowManager.setForceDozeBrightness(false);
        if (this.mStatusBar.getNavigationBarView() != null) {
            this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(false);
        }
        this.mStatusBar.notifyFpAuthModeChanged();
    }

    public void resetMode() {
        this.mMode = 0;
        this.mUpdateMonitor.setFingerprintMode(this.mMode);
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().resetGxzwUnlockMode();
        }
    }

    public void onStartedWakingUp() {
        synchronized (this) {
            if (this.mCancelingPendingLock) {
                this.mCancelingPendingLock = false;
                resetMode();
            }
        }
    }

    private void recordUnlockWay() {
        AnalyticsHelper.recordUnlockWay("fp");
        this.mKeyguardViewMediator.recordFingerprintUnlockState();
    }

    public synchronized boolean isCancelingPendingLock() {
        return this.mCancelingPendingLock;
    }

    public synchronized void resetCancelingPendingLock() {
        if (this.mCancelingPendingLock) {
            this.mCancelingPendingLock = false;
            this.mHandler.post(new Runnable() {
                public void run() {
                    FingerprintUnlockController.this.resetMode();
                }
            });
        }
    }
}
