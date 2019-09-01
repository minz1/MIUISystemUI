package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManagerGlobal;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.LatencyTracker;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.keyguard.faceunlock.FaceUnlockController;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import java.util.ArrayList;

public class StatusBarKeyguardViewManager implements RemoteInputController.Callback {
    private static String TAG = "StatusBarKeyguardViewManager";
    private KeyguardHostView.OnDismissAction mAfterKeyguardGoneAction;
    private final ArrayList<Runnable> mAfterKeyguardGoneRunnables = new ArrayList<>();
    private boolean mBackDown;
    private boolean mBackWithVolumeUp = false;
    protected KeyguardBouncer mBouncer;
    /* access modifiers changed from: private */
    public ViewGroup mContainer;
    protected final Context mContext;
    private boolean mDeferScrimFadeOut;
    private boolean mDeviceInteractive = false;
    private boolean mDeviceWillWakeUp;
    private boolean mDozing;
    private FaceUnlockController mFaceUnlockController;
    /* access modifiers changed from: private */
    public FingerprintUnlockController mFingerprintUnlockController;
    protected boolean mFirstUpdate = true;
    private KeyguardMonitorImpl mKeyguardMonitor;
    private boolean mLastBouncerDismissible;
    private boolean mLastBouncerShowing;
    private boolean mLastDeferScrimFadeOut;
    private boolean mLastDozing;
    protected boolean mLastOccluded;
    protected boolean mLastRemoteInputActive;
    protected boolean mLastShowing;
    protected LockPatternUtils mLockPatternUtils;
    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        public void run() {
            StatusBarKeyguardViewManager.this.mStatusBar.getNavigationBarView().getRootView().setVisibility(0);
        }
    };
    protected boolean mOccluded;
    protected boolean mRemoteInputActive;
    private boolean mScreenTurnedOn;
    /* access modifiers changed from: private */
    public ScrimController mScrimController;
    protected boolean mShowing;
    protected StatusBar mStatusBar;
    /* access modifiers changed from: private */
    public final StatusBarWindowManager mStatusBarWindowManager;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onEmergencyCallAction() {
            if (StatusBarKeyguardViewManager.this.mOccluded) {
                StatusBarKeyguardViewManager.this.reset(false);
            }
        }
    };
    protected ViewMediatorCallback mViewMediatorCallback;

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mStatusBarWindowManager = (StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class);
        this.mKeyguardMonitor = (KeyguardMonitorImpl) Dependency.get(KeyguardMonitor.class);
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateMonitorCallback);
    }

    public void registerStatusBar(StatusBar statusBar, ViewGroup container, ScrimController scrimController, FingerprintUnlockController fingerprintUnlockController, FaceUnlockController faceUnlockController, DismissCallbackRegistry dismissCallbackRegistry) {
        this.mStatusBar = statusBar;
        this.mContainer = container;
        this.mScrimController = scrimController;
        this.mFingerprintUnlockController = fingerprintUnlockController;
        this.mFaceUnlockController = faceUnlockController;
        this.mBouncer = SystemUIFactory.getInstance().createKeyguardBouncer(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils, container, dismissCallbackRegistry);
    }

    public void setKeyguardTransparent() {
        this.mStatusBar.setKeyguardTransparent();
    }

    public void show(Bundle options) {
        this.mShowing = true;
        this.mStatusBarWindowManager.setKeygaurdTransparent(false);
        this.mStatusBarWindowManager.setKeyguardShowing(true);
        this.mScrimController.abortKeyguardFadingOut();
        reset(true);
    }

    /* access modifiers changed from: protected */
    public void showBouncerOrKeyguard(boolean hideBouncerWhenShowing) {
        if (this.mBouncer.needsFullscreenBouncer()) {
            this.mStatusBar.hideKeyguard();
            this.mBouncer.show(true);
            return;
        }
        this.mStatusBar.showKeyguard();
        if (hideBouncerWhenShowing) {
            this.mBouncer.hide(false);
            this.mBouncer.prepare();
        }
    }

    private void showBouncer() {
        if (this.mShowing) {
            this.mBouncer.show(false);
        }
        updateStates();
    }

    public void dismissWithAction(KeyguardHostView.OnDismissAction r, Runnable cancelAction, boolean afterKeyguardGone) {
        if (this.mShowing) {
            if (!afterKeyguardGone) {
                this.mBouncer.showWithDismissAction(r, cancelAction);
            } else {
                this.mAfterKeyguardGoneAction = r;
                this.mBouncer.show(false);
            }
        }
        updateStates();
    }

    public void addAfterKeyguardGoneRunnable(Runnable runnable) {
        this.mAfterKeyguardGoneRunnables.add(runnable);
    }

    public void reset(boolean hideBouncerWhenShowing) {
        if (this.mShowing) {
            if (this.mOccluded) {
                this.mStatusBar.hideKeyguard();
                this.mStatusBar.stopWaitingForKeyguardExit();
                this.mBouncer.hide(false);
                this.mBouncer.prepare();
            } else {
                showBouncerOrKeyguard(hideBouncerWhenShowing);
            }
            KeyguardUpdateMonitor.getInstance(this.mContext).sendKeyguardReset();
            updateStates();
        }
    }

    public void onStartedGoingToSleep() {
        this.mStatusBar.onStartedGoingToSleep();
        this.mStatusBarWindowManager.setKeygaurdTransparent(false);
    }

    public void onFinishedGoingToSleep() {
        this.mDeviceInteractive = false;
        this.mStatusBar.onFinishedGoingToSleep();
        this.mBouncer.onFinishedGoingToSleep();
    }

    public void onStartedWakingUp() {
        Trace.beginSection("StatusBarKeyguardViewManager#onStartedWakingUp");
        this.mDeviceInteractive = true;
        this.mDeviceWillWakeUp = false;
        this.mStatusBar.onStartedWakingUp();
        Trace.endSection();
    }

    public void onScreenTurningOn() {
        Trace.beginSection("StatusBarKeyguardViewManager#onScreenTurningOn");
        this.mStatusBar.onScreenTurningOn();
        Trace.endSection();
    }

    public boolean isScreenTurnedOn() {
        return this.mScreenTurnedOn;
    }

    public void onScreenTurnedOn() {
        Trace.beginSection("StatusBarKeyguardViewManager#onScreenTurnedOn");
        this.mScreenTurnedOn = true;
        if (this.mDeferScrimFadeOut) {
            this.mDeferScrimFadeOut = false;
            animateScrimControllerKeyguardFadingOut(0, 0, true);
            updateStates();
        }
        this.mStatusBar.onScreenTurnedOn();
        Trace.endSection();
    }

    public void onRemoteInputActive(boolean active) {
        this.mRemoteInputActive = active;
        updateStates();
    }

    public void setDozing(boolean dozing) {
        this.mDozing = dozing;
        updateStates();
    }

    public void onScreenTurnedOff() {
        this.mScreenTurnedOn = false;
        this.mStatusBar.onScreenTurnedOff();
    }

    public void notifyDeviceWakeUpRequested() {
        this.mDeviceWillWakeUp = !this.mDeviceInteractive;
    }

    public void setNeedsInput(boolean needsInput) {
        this.mStatusBarWindowManager.setKeyguardNeedsInput(needsInput);
    }

    public void setOccluded(boolean occluded, boolean animate) {
        if (occluded != this.mOccluded) {
            this.mStatusBar.onKeyguardOccludedChanged(occluded);
        }
        boolean z = true;
        if (!occluded || this.mOccluded || !this.mShowing || !this.mStatusBar.isInLaunchTransition()) {
            this.mOccluded = occluded;
            if (this.mShowing) {
                StatusBar statusBar = this.mStatusBar;
                if (!animate || occluded) {
                    z = false;
                }
                statusBar.updateMediaMetaData(false, z);
            }
            this.mStatusBarWindowManager.setKeyguardOccluded(occluded);
            reset(false);
            return;
        }
        this.mOccluded = true;
        this.mStatusBar.fadeKeyguardAfterLaunchTransition(null, new Runnable() {
            public void run() {
                StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardOccluded(StatusBarKeyguardViewManager.this.mOccluded);
                StatusBarKeyguardViewManager.this.reset(true);
            }
        });
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void startPreHideAnimation(Runnable finishRunnable) {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.startPreHideAnimation(finishRunnable);
        } else if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    public void hide(long startTime, long fadeoutDuration) {
        long fadeoutDuration2;
        long fadeoutDuration3;
        long fadeoutDuration4;
        this.mShowing = false;
        if (KeyguardUpdateMonitor.getInstance(this.mContext).needsSlowUnlockTransition()) {
            fadeoutDuration2 = 2000;
        } else {
            fadeoutDuration2 = fadeoutDuration;
        }
        long delay = Math.max(0, (startTime - 48) - SystemClock.uptimeMillis());
        this.mStatusBar.onKeyguardDone();
        if (this.mStatusBar.isInLaunchTransition()) {
            this.mStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardShowing(false);
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                    StatusBarKeyguardViewManager.this.mBouncer.hide(true);
                    StatusBarKeyguardViewManager.this.updateStates();
                    StatusBarKeyguardViewManager.this.mScrimController.animateKeyguardFadingOut(100, 300, null, false);
                }
            }, new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBar.hideKeyguard();
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.keyguardGone();
                    StatusBarKeyguardViewManager.this.executeAfterKeyguardGoneAction();
                }
            });
            long j = delay;
            return;
        }
        executeAfterKeyguardGoneAction();
        boolean unlockByGxzw = MiuiKeyguardUtils.isGxzwSensor() && MiuiGxzwManager.getInstance().isUnlockByGxzw();
        boolean wakeUnlockPulsing = this.mFingerprintUnlockController.getMode() == 2;
        boolean modeUnlock = this.mFingerprintUnlockController.getMode() == 5;
        if (wakeUnlockPulsing) {
            delay = 0;
            fadeoutDuration2 = 240;
        }
        long delay2 = delay;
        long fadeoutDuration5 = fadeoutDuration2;
        this.mStatusBar.setKeyguardFadingAway(startTime, delay2, fadeoutDuration5);
        this.mFingerprintUnlockController.startKeyguardFadingAway();
        this.mBouncer.hide(true);
        if (wakeUnlockPulsing) {
            this.mStatusBarWindowManager.setKeyguardFadingAway(true);
            this.mStatusBar.fadeKeyguardWhilePulsing();
            long fadeoutDuration6 = fadeoutDuration5;
            animateScrimControllerKeyguardFadingOut(delay2, fadeoutDuration6, new Runnable() {
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBar.hideKeyguard();
                }
            }, false);
            long j2 = delay2;
            fadeoutDuration3 = fadeoutDuration6;
        } else {
            long fadeoutDuration7 = fadeoutDuration5;
            long delay3 = delay2;
            if (!modeUnlock || !unlockByGxzw || this.mOccluded || MiuiGxzwManager.getInstance().isFodFastUnlock()) {
                long delay4 = delay3;
                fadeoutDuration3 = fadeoutDuration7;
                this.mFingerprintUnlockController.startKeyguardFadingAway();
                this.mStatusBar.setKeyguardFadingAway(startTime, delay4, fadeoutDuration3);
                if (!this.mStatusBar.hideKeyguard()) {
                    this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                    if (this.mFingerprintUnlockController.getMode() != 1) {
                        fadeoutDuration4 = fadeoutDuration3;
                        animateScrimControllerKeyguardFadingOut(delay4, fadeoutDuration4, false);
                    } else if (!this.mScreenTurnedOn) {
                        this.mDeferScrimFadeOut = true;
                        long j3 = delay4;
                    } else {
                        fadeoutDuration4 = fadeoutDuration3;
                        long j4 = delay4;
                        animateScrimControllerKeyguardFadingOut(0, 0, true);
                    }
                    fadeoutDuration3 = fadeoutDuration4;
                } else {
                    long j5 = fadeoutDuration3;
                    this.mScrimController.animateGoingToFullShade(delay4, fadeoutDuration3);
                    this.mStatusBar.finishKeyguardFadingAway();
                    this.mFingerprintUnlockController.finishKeyguardFadingAway();
                }
            } else {
                this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                this.mStatusBar.setKeyguardFadingAway(startTime, 300, 0);
                this.mStatusBar.fadeKeyguardWhenUnlockByGxzw(new Runnable() {
                    public void run() {
                        StatusBarKeyguardViewManager.this.animateScrimControllerKeyguardFadingOut(0, 0, new Runnable() {
                            public void run() {
                                StatusBarKeyguardViewManager.this.mStatusBar.hideKeyguard();
                            }
                        }, false);
                    }
                });
                long j6 = delay3;
                fadeoutDuration3 = fadeoutDuration7;
            }
        }
        updateStates();
        this.mStatusBarWindowManager.setKeyguardShowing(false);
        this.mViewMediatorCallback.keyguardGone();
        long j7 = fadeoutDuration3;
    }

    public void onDensityOrFontScaleChanged() {
        this.mBouncer.hide(true);
    }

    private void animateScrimControllerKeyguardFadingOut(long delay, long duration, boolean skipFirstFrame) {
        animateScrimControllerKeyguardFadingOut(delay, duration, null, skipFirstFrame);
    }

    /* access modifiers changed from: private */
    public void animateScrimControllerKeyguardFadingOut(long delay, long duration, Runnable endRunnable, boolean skipFirstFrame) {
        Trace.asyncTraceBegin(8, "Fading out", 0);
        final Runnable runnable = endRunnable;
        this.mScrimController.animateKeyguardFadingOut(delay, duration, new Runnable() {
            public void run() {
                if (runnable != null) {
                    runnable.run();
                }
                StatusBarKeyguardViewManager.this.mContainer.postDelayed(new Runnable() {
                    public void run() {
                        StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                    }
                }, 100);
                StatusBarKeyguardViewManager.this.mStatusBar.finishKeyguardFadingAway();
                StatusBarKeyguardViewManager.this.mFingerprintUnlockController.finishKeyguardFadingAway();
                WindowManagerGlobal.getInstance().trimMemory(20);
                Trace.asyncTraceEnd(8, "Fading out", 0);
            }
        }, skipFirstFrame);
        if (this.mFingerprintUnlockController.getMode() == 1 && LatencyTracker.isEnabled(this.mContext)) {
            DejankUtils.postAfterTraversal(new Runnable() {
                public void run() {
                    LatencyTracker.getInstance(StatusBarKeyguardViewManager.this.mContext).onActionEnd(2);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void executeAfterKeyguardGoneAction() {
        if (this.mAfterKeyguardGoneAction != null) {
            this.mAfterKeyguardGoneAction.onDismiss();
            this.mAfterKeyguardGoneAction = null;
        }
        for (int i = 0; i < this.mAfterKeyguardGoneRunnables.size(); i++) {
            this.mAfterKeyguardGoneRunnables.get(i).run();
        }
        this.mAfterKeyguardGoneRunnables.clear();
    }

    public void dismissAndCollapse() {
        this.mStatusBar.executeRunnableDismissingKeyguard(null, null, true, false, true);
    }

    public void dismiss() {
        showBouncer();
    }

    public boolean isSecure() {
        return this.mBouncer.isSecure();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean onBackPressed() {
        if (!this.mBouncer.isShowing()) {
            return false;
        }
        if (this.mBouncer.onBackPressed()) {
            return true;
        }
        this.mStatusBar.endAffordanceLaunch();
        reset(true);
        return true;
    }

    public boolean isBouncerShowing() {
        return this.mBouncer.isShowing();
    }

    private long getNavBarShowDelay() {
        if (this.mStatusBar.isKeyguardFadingAway()) {
            return this.mStatusBar.getKeyguardFadingAwayDelay();
        }
        if (this.mBouncer.isShowing()) {
            return 320;
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    public void updateStates() {
        int vis = this.mContainer.getSystemUiVisibility();
        boolean showing = this.mShowing;
        boolean occluded = this.mOccluded;
        boolean bouncerShowing = this.mBouncer.isShowing();
        boolean z = true;
        boolean bouncerDismissible = !this.mBouncer.isFullscreenBouncer();
        boolean remoteInputActive = this.mRemoteInputActive;
        if ((bouncerDismissible || !showing || remoteInputActive) != (this.mLastBouncerDismissible || !this.mLastShowing || this.mLastRemoteInputActive) || this.mFirstUpdate) {
            if (bouncerDismissible || !showing || remoteInputActive) {
                this.mContainer.setSystemUiVisibility(-4194305 & vis);
            } else {
                this.mContainer.setSystemUiVisibility(4194304 | vis);
            }
        }
        boolean navBarVisible = isNavBarVisible();
        if ((navBarVisible != getLastNavBarVisible() || this.mFirstUpdate) && this.mStatusBar.getNavigationBarView() != null) {
            if (navBarVisible) {
                long delay = getNavBarShowDelay();
                if (delay == 0) {
                    this.mMakeNavigationBarVisibleRunnable.run();
                } else {
                    this.mContainer.postOnAnimationDelayed(this.mMakeNavigationBarVisibleRunnable, delay);
                }
            } else {
                this.mContainer.removeCallbacks(this.mMakeNavigationBarVisibleRunnable);
                this.mStatusBar.getNavigationBarView().getRootView().setVisibility(8);
            }
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            this.mStatusBarWindowManager.setBouncerShowing(bouncerShowing);
            this.mStatusBar.setBouncerShowing(bouncerShowing);
            this.mScrimController.setBouncerShowing(bouncerShowing);
        }
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if ((showing && !occluded) != (this.mLastShowing && !this.mLastOccluded) || this.mFirstUpdate) {
            if (!showing || occluded) {
                z = false;
            }
            updateMonitor.onKeyguardVisibilityChanged(z);
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            updateMonitor.sendKeyguardBouncerChanged(bouncerShowing);
        }
        this.mFirstUpdate = false;
        this.mLastShowing = showing;
        this.mLastOccluded = occluded;
        this.mLastBouncerShowing = bouncerShowing;
        this.mLastBouncerDismissible = bouncerDismissible;
        this.mLastRemoteInputActive = remoteInputActive;
        this.mLastDeferScrimFadeOut = this.mDeferScrimFadeOut;
        this.mLastDozing = this.mDozing;
        this.mStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    /* access modifiers changed from: protected */
    public boolean isNavBarVisible() {
        return (((!this.mShowing || this.mOccluded) && !this.mDozing) || this.mBouncer.isShowing() || this.mRemoteInputActive) && !this.mDeferScrimFadeOut;
    }

    /* access modifiers changed from: protected */
    public boolean getLastNavBarVisible() {
        return (((!this.mLastShowing || this.mLastOccluded) && !this.mLastDozing) || this.mLastBouncerShowing || this.mLastRemoteInputActive) && !this.mLastDeferScrimFadeOut;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mBouncer.shouldDismissOnMenuPressed();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        if (interceptKey(event)) {
            return true;
        }
        return this.mBouncer.interceptMediaKey(event);
    }

    /* JADX INFO: finally extract failed */
    public boolean interceptKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == 0) {
            if (keyCode == 4) {
                this.mBackDown = true;
                setBackWithVolumeUp(false);
            } else if (keyCode == 24) {
                setBackWithVolumeUp(this.mBackDown);
            } else {
                this.mBackDown = false;
            }
        } else if (event.getAction() == 1) {
            if (keyCode == 24) {
                try {
                    if (this.mBackDown) {
                        this.mBackDown = false;
                        dismissAndCollapse();
                        Log.d(TAG, "Unlock Screen by pressing back + volume_up");
                        this.mBackDown = false;
                        return true;
                    }
                } catch (Throwable th) {
                    this.mBackDown = false;
                    throw th;
                }
            } else if (keyCode == 4) {
                if (this.mBackWithVolumeUp) {
                    setBackWithVolumeUp(false);
                    this.mBackDown = false;
                    return true;
                }
                this.mBackDown = false;
                return false;
            }
            this.mBackDown = false;
        }
        return false;
    }

    private void setBackWithVolumeUp(boolean backWithVolumeUp) {
        if (this.mBackWithVolumeUp != backWithVolumeUp) {
            this.mBackWithVolumeUp = backWithVolumeUp;
            this.mKeyguardMonitor.notifySkipVolumeDialog(this.mBackWithVolumeUp);
        }
    }

    public void readyForKeyguardDone() {
        this.mViewMediatorCallback.readyForKeyguardDone();
    }

    public boolean isSecure(int userId) {
        return this.mBouncer.isSecure() || this.mLockPatternUtils.isSecure(userId);
    }

    public void keyguardGoingAway() {
        this.mStatusBar.keyguardGoingAway();
    }

    public void animateCollapsePanels(float speedUpFactor) {
        this.mStatusBar.animateCollapsePanels(0, true, false, speedUpFactor);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        this.mBouncer.notifyKeyguardAuthenticated(strongAuth);
    }

    public void showBouncerMessage(String message, int color) {
        this.mBouncer.showMessage(message, color);
    }

    public void showBouncerMessage(String title, String message, int color) {
        this.mBouncer.showMessage(title, message, color);
    }

    public void applyHintAnimation(long offset) {
        this.mBouncer.applyHintAnimation(offset);
    }

    public ViewRootImpl getViewRootImpl() {
        return this.mStatusBar.getStatusBarView().getViewRootImpl();
    }

    public void onRemoteInputSent(NotificationData.Entry entry) {
    }
}
