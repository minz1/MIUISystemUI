package com.android.systemui.statusbar.phone;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.TimeUtils;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.Interpolators;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LightBarTransitionsController implements Dumpable, CommandQueue.Callbacks {
    private final DarkIntensityApplier mApplier;
    private float mDarkIntensity;
    private final Handler mHandler;
    private final KeyguardMonitor mKeyguardMonitor;
    private float mNextDarkIntensity;
    private float mPendingDarkIntensity;
    private ValueAnimator mTintAnimator;
    private boolean mTintChangePending;
    /* access modifiers changed from: private */
    public boolean mTransitionDeferring;
    private final Runnable mTransitionDeferringDoneRunnable = new Runnable() {
        public void run() {
            boolean unused = LightBarTransitionsController.this.mTransitionDeferring = false;
        }
    };
    private long mTransitionDeferringDuration;
    private long mTransitionDeferringStartTime;
    private boolean mTransitionPending;

    public interface DarkIntensityApplier {
        void applyDarkIntensity(float f);
    }

    public LightBarTransitionsController(Context context, DarkIntensityApplier applier) {
        this.mApplier = applier;
        this.mHandler = new Handler();
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        ((CommandQueue) SystemUI.getComponent(context, CommandQueue.class)).addCallbacks(this);
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
    }

    public void disable(int state1, int state2, boolean animate) {
    }

    public void animateExpandNotificationsPanel() {
    }

    public void animateCollapsePanels(int flags) {
    }

    public void animateExpandSettingsPanel(String obj) {
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
    }

    public void topAppWindowChanged(boolean visible) {
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
    }

    public void toggleRecentApps() {
    }

    public void toggleSplitScreen() {
    }

    public void preloadRecentApps() {
    }

    public void dismissKeyboardShortcutsMenu() {
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
    }

    public void cancelPreloadRecentApps() {
    }

    public void setWindowState(int window, int state) {
    }

    public void showScreenPinningRequest(int taskId) {
    }

    public void appTransitionPending(boolean forced) {
        if (!this.mKeyguardMonitor.isKeyguardGoingAway() || forced) {
            this.mTransitionPending = true;
        }
    }

    public void appTransitionCancelled() {
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, 0, 120);
        }
        this.mTransitionPending = false;
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
        if (!this.mKeyguardMonitor.isKeyguardGoingAway() || forced) {
            if (this.mTransitionPending && this.mTintChangePending) {
                this.mTintChangePending = false;
                animateIconTint(this.mPendingDarkIntensity, Math.max(0, startTime - SystemClock.uptimeMillis()), duration);
            } else if (this.mTransitionPending) {
                this.mTransitionDeferring = true;
                this.mTransitionDeferringStartTime = startTime;
                this.mTransitionDeferringDuration = duration;
                this.mHandler.removeCallbacks(this.mTransitionDeferringDoneRunnable);
                this.mHandler.postAtTime(this.mTransitionDeferringDoneRunnable, startTime);
            }
            this.mTransitionPending = false;
        }
    }

    public void appTransitionFinished() {
    }

    public void showAssistDisclosure() {
    }

    public void startAssist(Bundle args) {
    }

    public void showPictureInPictureMenu() {
    }

    public void addQsTile(ComponentName tile) {
    }

    public void remQsTile(ComponentName tile) {
    }

    public void clickTile(ComponentName tile) {
    }

    public void showFingerprintDialog(SomeArgs args) {
    }

    public void onFingerprintAuthenticated() {
    }

    public void onFingerprintHelp(String message) {
    }

    public void onFingerprintError(String error) {
    }

    public void hideFingerprintDialog() {
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
    }

    public void setIconsDark(boolean dark, boolean animate) {
        float f = 0.0f;
        float f2 = 1.0f;
        if (!animate) {
            if (this.mTintAnimator != null) {
                this.mTintAnimator.cancel();
            }
            if (dark) {
                f = 1.0f;
            }
            setIconTintInternal(f);
        } else if (this.mTransitionPending) {
            if (dark) {
                f = 1.0f;
            }
            deferIconTintChange(f);
        } else if (this.mTransitionDeferring) {
            if (!dark) {
                f2 = 0.0f;
            }
            animateIconTint(f2, Math.max(0, this.mTransitionDeferringStartTime - SystemClock.uptimeMillis()), this.mTransitionDeferringDuration);
        } else {
            if (!dark) {
                f2 = 0.0f;
            }
            animateIconTint(f2, 0, 120);
        }
    }

    private void deferIconTintChange(float darkIntensity) {
        if (!this.mTintChangePending || darkIntensity != this.mPendingDarkIntensity) {
            this.mTintChangePending = true;
            this.mPendingDarkIntensity = darkIntensity;
        }
    }

    private void animateIconTint(float targetDarkIntensity, long delay, long duration) {
        if (this.mTintAnimator != null) {
            this.mTintAnimator.cancel();
        }
        if (this.mDarkIntensity != targetDarkIntensity) {
            this.mNextDarkIntensity = targetDarkIntensity;
            this.mTintAnimator = ValueAnimator.ofFloat(new float[]{this.mDarkIntensity, targetDarkIntensity});
            this.mTintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    LightBarTransitionsController.this.setIconTintInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            });
            this.mTintAnimator.setDuration(duration);
            this.mTintAnimator.setStartDelay(delay);
            this.mTintAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            this.mTintAnimator.start();
        }
    }

    /* access modifiers changed from: private */
    public void setIconTintInternal(float darkIntensity) {
        this.mDarkIntensity = darkIntensity;
        this.mApplier.applyDarkIntensity(darkIntensity);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("  mTransitionDeferring=");
        pw.print(this.mTransitionDeferring);
        if (this.mTransitionDeferring) {
            pw.println();
            pw.print("   mTransitionDeferringStartTime=");
            pw.println(TimeUtils.formatUptime(this.mTransitionDeferringStartTime));
            pw.print("   mTransitionDeferringDuration=");
            TimeUtils.formatDuration(this.mTransitionDeferringDuration, pw);
            pw.println();
        }
        pw.print("  mTransitionPending=");
        pw.print(this.mTransitionPending);
        pw.print(" mTintChangePending=");
        pw.println(this.mTintChangePending);
        pw.print("  mPendingDarkIntensity=");
        pw.print(this.mPendingDarkIntensity);
        pw.print(" mDarkIntensity=");
        pw.print(this.mDarkIntensity);
        pw.print(" mNextDarkIntensity=");
        pw.println(this.mNextDarkIntensity);
    }
}
