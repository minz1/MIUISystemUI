package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerCompat;
import android.content.Context;
import android.content.res.Resources;
import android.os.Binder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.RemoteInputController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class StatusBarWindowManager implements Dumpable, RemoteInputController.Callback {
    private int mBarHeight;
    private List<BlurRatioChangedListener> mBlurRatioListeners = new ArrayList();
    private final Context mContext;
    private final State mCurrentState = new State();
    private boolean mHasTopUi;
    private boolean mHasTopUiChanged;
    private final boolean mKeyguardScreenRotation;
    private OtherwisedCollapsedListener mListener;
    private WindowManager.LayoutParams mLp;
    private WindowManager.LayoutParams mLpChanged;
    private float mRestoredBlurRatio;
    private final float mScreenBrightnessDoze;
    private View mStatusBarView;
    private final WindowManager mWindowManager;

    public interface BlurRatioChangedListener {
        void onBlurRatioChanged(float f);
    }

    public interface OtherwisedCollapsedListener {
        void setWouldOtherwiseCollapse(boolean z);
    }

    private static class State {
        boolean backdropShowing;
        float blurRatio;
        boolean bouncerShowing;
        boolean dozing;
        boolean forceCollapsed;
        boolean forceDozeBrightness;
        boolean forcePluginOpen;
        boolean forceStatusBarVisible;
        boolean forceUserActivity;
        boolean headsUpShowing;
        boolean keygaurdTransparent;
        boolean keyguardFadingAway;
        boolean keyguardNeedsInput;
        boolean keyguardOccluded;
        boolean keyguardShowing;
        boolean panelExpanded;
        boolean panelVisible;
        boolean qsExpanded;
        boolean remoteInputActive;
        boolean statusBarFocusable;
        int statusBarState;

        private State() {
        }

        /* access modifiers changed from: private */
        public boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Window State {");
            result.append("\n");
            for (Field field : getClass().getDeclaredFields()) {
                result.append("  ");
                try {
                    result.append(field.getName());
                    result.append(": ");
                    result.append(field.get(this));
                } catch (IllegalAccessException e) {
                }
                result.append("\n");
            }
            result.append("}");
            return result.toString();
        }
    }

    public StatusBarWindowManager(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
        this.mScreenBrightnessDoze = ((float) this.mContext.getResources().getInteger(17694860)) / 255.0f;
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        Resources res = this.mContext.getResources();
        if (SystemProperties.getBoolean("lockscreen.rot_override", false) || res.getBoolean(R.bool.config_enableLockScreenRotation)) {
            return true;
        }
        return false;
    }

    public void add(View statusBarView, int barHeight) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, barHeight, 2000, -2138832824, -3);
        this.mLp = layoutParams;
        this.mLp.token = new Binder();
        this.mLp.flags |= 16777216;
        this.mLp.gravity = 48;
        this.mLp.softInputMode = 16;
        this.mLp.setTitle("StatusBar");
        this.mLp.packageName = this.mContext.getPackageName();
        WindowManagerCompat.setLayoutInDisplayCutoutMode(this.mLp, 1);
        this.mStatusBarView = statusBarView;
        this.mBarHeight = barHeight;
        this.mWindowManager.addView(this.mStatusBarView, this.mLp);
        this.mLpChanged = new WindowManager.LayoutParams();
        this.mLpChanged.copyFrom(this.mLp);
    }

    private void applyKeyguardFlags(State state) {
        if (state.keyguardShowing) {
            this.mLpChanged.privateFlags |= 1024;
        } else {
            this.mLpChanged.privateFlags &= -1025;
        }
        if (!state.keyguardShowing || !WallpaperAuthorityUtils.isThemeLockLiveWallpaper(this.mContext) || state.dozing) {
            this.mLpChanged.flags &= -1048577;
        } else {
            this.mLpChanged.flags |= 1048576;
        }
        if ((state.isKeyguardShowingAndNotOccluded() || state.keyguardFadingAway) && state.keygaurdTransparent) {
            this.mLpChanged.alpha = 0.0f;
            this.mLpChanged.flags |= 16;
            return;
        }
        this.mLpChanged.alpha = 1.0f;
        this.mLpChanged.flags &= -17;
        this.mCurrentState.keygaurdTransparent = false;
    }

    private void adjustScreenOrientation(State state) {
        if (!state.isKeyguardShowingAndNotOccluded()) {
            this.mLpChanged.screenOrientation = -1;
        } else if (this.mKeyguardScreenRotation) {
            this.mLpChanged.screenOrientation = 2;
        } else {
            this.mLpChanged.screenOrientation = 5;
        }
    }

    private void applyFocusableFlag(State state) {
        boolean panelFocusable = state.statusBarFocusable && state.panelExpanded;
        if ((state.bouncerShowing && (state.keyguardOccluded || state.keyguardNeedsInput)) || (StatusBar.ENABLE_REMOTE_INPUT && state.remoteInputActive)) {
            this.mLpChanged.flags &= -9;
            WindowManager.LayoutParams layoutParams = this.mLpChanged;
            layoutParams.flags = -131073 & layoutParams.flags;
        } else if (state.isKeyguardShowingAndNotOccluded() || panelFocusable) {
            this.mLpChanged.flags &= -9;
            this.mLpChanged.flags |= 131072;
        } else {
            this.mLpChanged.flags |= 8;
            WindowManager.LayoutParams layoutParams2 = this.mLpChanged;
            layoutParams2.flags = -131073 & layoutParams2.flags;
        }
        this.mLpChanged.softInputMode = 16;
    }

    private void applyExpandedFlag(State state) {
        WindowManagerCompat.applyExpandedFlag(state.panelExpanded || state.isKeyguardShowingAndNotOccluded() || state.bouncerShowing || (StatusBar.ENABLE_REMOTE_INPUT && state.remoteInputActive), this.mLpChanged);
    }

    private void applyHeight(State state) {
        boolean expanded = isExpanded(state);
        if (state.forcePluginOpen) {
            this.mListener.setWouldOtherwiseCollapse(expanded);
            expanded = true;
        }
        if (expanded) {
            this.mLpChanged.height = -1;
            return;
        }
        this.mLpChanged.height = this.mBarHeight;
    }

    private boolean isExpanded(State state) {
        return !state.forceCollapsed && (state.isKeyguardShowingAndNotOccluded() || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing || state.headsUpShowing);
    }

    private void applyFitsSystemWindows(State state) {
        boolean fitsSystemWindows = !state.isKeyguardShowingAndNotOccluded();
        if (this.mStatusBarView.getFitsSystemWindows() != fitsSystemWindows) {
            this.mStatusBarView.setFitsSystemWindows(fitsSystemWindows);
            this.mStatusBarView.requestApplyInsets();
        }
    }

    private void applyUserActivityTimeout(State state) {
        if (!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 1 || state.qsExpanded) {
            this.mLpChanged.userActivityTimeout = -1;
        } else {
            this.mLpChanged.userActivityTimeout = 10000;
        }
    }

    private void applyInputFeatures(State state) {
        if (!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 1 || state.qsExpanded || state.forceUserActivity) {
            this.mLpChanged.inputFeatures &= -5;
            return;
        }
        this.mLpChanged.inputFeatures |= 4;
    }

    private void apply(State state) {
        applyKeyguardFlags(state);
        applyForceStatusBarVisibleFlag(state);
        applyFocusableFlag(state);
        applyExpandedFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        applyModalFlag(state);
        applyBrightness(state);
        applyHasTopUi(state);
        applyBlurRatio(state);
        applySleepToken(state);
        if (this.mLp.copyFrom(this.mLpChanged) != 0) {
            this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
        }
        if (this.mHasTopUi != this.mHasTopUiChanged) {
            try {
                ActivityManagerCompat.setHasTopUi(this.mHasTopUiChanged);
            } catch (RemoteException e) {
                Log.e("StatusBarWindowManager", "Failed to call setHasTopUi", e);
            }
            this.mHasTopUi = this.mHasTopUiChanged;
        }
    }

    private void applyForceStatusBarVisibleFlag(State state) {
        if (state.forceStatusBarVisible) {
            this.mLpChanged.privateFlags |= 4096;
            return;
        }
        this.mLpChanged.privateFlags &= -4097;
    }

    private void applyModalFlag(State state) {
        if (state.headsUpShowing) {
            this.mLpChanged.flags |= 32;
            return;
        }
        this.mLpChanged.flags &= -33;
    }

    private void applyBrightness(State state) {
        if (state.forceDozeBrightness) {
            this.mLpChanged.screenBrightness = this.mScreenBrightnessDoze;
            return;
        }
        this.mLpChanged.screenBrightness = -1.0f;
    }

    private void applyHasTopUi(State state) {
        this.mHasTopUiChanged = isExpanded(state);
    }

    private void applyBlurRatio(State state) {
        this.mLpChanged.blurRatio = state.blurRatio;
        if (state.blurRatio == 0.0f) {
            this.mLpChanged.flags &= -5;
        } else {
            this.mLpChanged.flags |= 4;
        }
        for (BlurRatioChangedListener listener : this.mBlurRatioListeners) {
            listener.onBlurRatioChanged(state.blurRatio);
        }
    }

    private void applySleepToken(State state) {
        WindowManagerCompat.applySleepToken(state.dozing, this.mLpChanged);
    }

    public void toggleBlurBackgroundByBrightnessMirror(boolean blur) {
        if (!blur) {
            if (this.mCurrentState.blurRatio > 0.0f) {
                this.mRestoredBlurRatio = this.mCurrentState.blurRatio;
                setBlurRatio(0.0f);
            }
        } else if (this.mCurrentState.blurRatio == 0.0f) {
            setBlurRatio(this.mRestoredBlurRatio);
        }
    }

    public void setKeyguardShowing(boolean showing) {
        this.mCurrentState.keyguardShowing = showing;
        apply(this.mCurrentState);
    }

    public void setKeyguardOccluded(boolean occluded) {
        this.mCurrentState.keyguardOccluded = occluded;
        apply(this.mCurrentState);
    }

    public void setKeygaurdTransparent(boolean transparent) {
        this.mCurrentState.keygaurdTransparent = transparent;
        apply(this.mCurrentState);
    }

    public void setKeyguardNeedsInput(boolean needsInput) {
        this.mCurrentState.keyguardNeedsInput = needsInput;
        apply(this.mCurrentState);
    }

    public void setPanelVisible(boolean visible) {
        this.mCurrentState.panelVisible = visible;
        this.mCurrentState.statusBarFocusable = visible;
        apply(this.mCurrentState);
    }

    public void setStatusBarFocusable(boolean focusable) {
        this.mCurrentState.statusBarFocusable = focusable;
        apply(this.mCurrentState);
    }

    public void setBouncerShowing(boolean showing) {
        this.mCurrentState.bouncerShowing = showing;
        apply(this.mCurrentState);
    }

    public void setBackdropShowing(boolean showing) {
        this.mCurrentState.backdropShowing = showing;
        apply(this.mCurrentState);
    }

    public void setKeyguardFadingAway(boolean keyguardFadingAway) {
        this.mCurrentState.keyguardFadingAway = keyguardFadingAway;
        apply(this.mCurrentState);
    }

    public void setQsExpanded(boolean expanded) {
        this.mCurrentState.qsExpanded = expanded;
        apply(this.mCurrentState);
    }

    public void setHeadsUpShowing(boolean showing) {
        this.mCurrentState.headsUpShowing = showing;
        apply(this.mCurrentState);
    }

    public void setStatusBarState(int state) {
        this.mCurrentState.statusBarState = state;
        apply(this.mCurrentState);
    }

    public void setBlurRatio(float blurRatio) {
        this.mCurrentState.blurRatio = blurRatio;
        apply(this.mCurrentState);
    }

    public void setForceStatusBarVisible(boolean forceStatusBarVisible) {
        this.mCurrentState.forceStatusBarVisible = forceStatusBarVisible;
        apply(this.mCurrentState);
    }

    public void setForceWindowCollapsed(boolean force) {
        this.mCurrentState.forceCollapsed = force;
        apply(this.mCurrentState);
    }

    public void setPanelExpanded(boolean isExpanded) {
        this.mCurrentState.panelExpanded = isExpanded;
        apply(this.mCurrentState);
    }

    public void onRemoteInputActive(boolean remoteInputActive) {
        this.mCurrentState.remoteInputActive = remoteInputActive;
        apply(this.mCurrentState);
    }

    public void onRemoteInputSent(NotificationData.Entry entry) {
    }

    public void setForceDozeBrightness(boolean forceDozeBrightness) {
        this.mCurrentState.forceDozeBrightness = forceDozeBrightness;
        apply(this.mCurrentState);
    }

    public void setDozing(boolean dozing) {
        this.mCurrentState.dozing = dozing;
        apply(this.mCurrentState);
    }

    public void setBarHeight(int barHeight) {
        this.mBarHeight = barHeight;
        apply(this.mCurrentState);
    }

    public void setForcePluginOpen(boolean forcePluginOpen) {
        this.mCurrentState.forcePluginOpen = forcePluginOpen;
        apply(this.mCurrentState);
    }

    public void setStateListener(OtherwisedCollapsedListener listener) {
        this.mListener = listener;
    }

    public void addBlurRatioListener(BlurRatioChangedListener listener) {
        this.mBlurRatioListeners.add(listener);
    }

    public void removeBlurRatioListener(BlurRatioChangedListener listener) {
        this.mBlurRatioListeners.remove(listener);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("StatusBarWindowManager state:");
        pw.println(this.mCurrentState);
    }
}
