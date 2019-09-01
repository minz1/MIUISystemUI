package com.android.keyguard.faceunlock;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.phone.DozeScrimController;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import miui.os.Build;

public class FaceUnlockController extends KeyguardUpdateMonitorCallback {
    private final Context mContext;
    private DozeScrimController mDozeScrimController;
    private Handler mHandler = new Handler();
    private KeyguardViewMediator mKeyguardViewMediator;
    private int mMode;
    /* access modifiers changed from: private */
    public int mPendingAuthenticatedUserId = -1;
    private PowerManager mPowerManager;
    private ScrimController mScrimController;
    private StatusBar mStatusBar;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarWindowManager mStatusBarWindowManager;
    private final UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitor mUpdateMonitor;

    public FaceUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache) {
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

    public void onFaceAuthenticated() {
        Trace.beginSection("FingerprintUnlockController#onFaceAuthenticated");
        boolean wasDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        this.mMode = calculateMode(0);
        Class<?> cls = getClass();
        PanelBar.LOG((Class) cls, "calculateMode userid=0;mode=" + this.mMode);
        if (!wasDeviceInteractive) {
            Log.i("FaceUnlockController", "fp wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:FACE");
        }
        this.mUpdateMonitor.setFaceUnlockMode(this.mMode);
        Trace.beginSection("release wake-and-unlock");
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
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mKeyguardViewMediator.onWakeAndUnlocking();
                this.mScrimController.setWakeAndUnlocking();
                this.mDozeScrimController.setWakeAndUnlocking();
                if (this.mStatusBar.getNavigationBarView() != null) {
                    this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(true);
                }
                Trace.endSection();
                break;
            case 3:
                Trace.beginSection("MODE_UNLOCK or MODE_SHOW_BOUNCER");
                if (!wasDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
                Trace.endSection();
                break;
            case 5:
                Trace.beginSection("MODE_UNLOCK");
                if (!wasDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                if (this.mStatusBar.canPanelBeCollapsed() || ("perseus".equals(Build.DEVICE) && this.mKeyguardViewMediator.isShowingAndOccluded())) {
                    this.mStatusBarWindowManager.setStatusBarFocusable(false);
                    if (this.mUpdateMonitor.isStayScreenFaceUnlockSuccess()) {
                        this.mUpdateMonitor.updateFingerprintListeningState();
                    } else {
                        this.mKeyguardViewMediator.keyguardDone();
                        sendFaceUnlcokSucceedBroadcast();
                    }
                }
                Trace.endSection();
                break;
            case 6:
                Trace.beginSection("MODE_DISMISS");
                this.mStatusBarKeyguardViewManager.notifyKeyguardAuthenticated(false);
                Trace.endSection();
                break;
        }
        if (this.mMode != 2) {
            this.mStatusBarWindowManager.setForceDozeBrightness(false);
        }
        this.mStatusBar.notifyFpAuthModeChanged();
        Trace.endSection();
    }

    public void onStartedGoingToSleep(int why) {
        this.mPendingAuthenticatedUserId = -1;
    }

    public void onFinishedGoingToSleep(int why) {
        Trace.beginSection("FingerprintUnlockController#onFinishedGoingToSleep");
        if (this.mPendingAuthenticatedUserId != -1) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    FaceUnlockController.this.onFingerprintAuthenticated(FaceUnlockController.this.mPendingAuthenticatedUserId);
                }
            });
        }
        this.mPendingAuthenticatedUserId = -1;
        Trace.endSection();
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

    public void onFaceAuthFailed(boolean hasFace) {
        if (hasFace && !MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext)) {
            cleanup();
            this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
        }
    }

    public void onFaceLocked() {
        this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
    }

    private void cleanup() {
    }

    public void unblockScreenOn() {
        this.mKeyguardViewMediator.unblockScreenOn();
    }

    private void sendFaceUnlcokSucceedBroadcast() {
        if (MiuiKeyguardUtils.isScreenTurnOnDelayed()) {
            Intent intent = new Intent("com.miui.keyguard.face_unlock_succeed");
            intent.addFlags(603979776);
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
        }
    }

    public void restartFaceUnlock() {
        this.mUpdateMonitor.setFaceUnlockStarted(false);
        this.mUpdateMonitor.startFaceUnlock();
    }

    public void resetFaceUnlockMode() {
        this.mMode = 0;
        this.mUpdateMonitor.setFaceUnlockMode(this.mMode);
    }
}
