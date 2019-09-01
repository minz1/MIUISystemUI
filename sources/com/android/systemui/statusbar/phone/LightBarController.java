package com.android.systemui.statusbar.phone;

import android.graphics.Rect;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LightBarController implements Dumpable, BatteryController.BatteryStateChangeCallback {
    private final BatteryController mBatteryController = ((BatteryController) Dependency.get(BatteryController.class));
    private boolean mDockedLight;
    private Rect mDockedStackBounds = new Rect();
    private int mDockedStackVisibility;
    private boolean mDriveMode;
    private FingerprintUnlockController mFingerprintUnlockController;
    private boolean mForceBlack;
    private Rect mFullScreenStackBounds = new Rect();
    private boolean mFullscreenLight;
    private int mFullscreenStackVisibility;
    private boolean mHasLightNavigationBar;
    private final Rect mLastDockedBounds = new Rect();
    private final Rect mLastFullscreenBounds = new Rect();
    private int mLastNavigationBarMode;
    private int mLastStatusBarMode;
    private LightBarTransitionsController mNavigationBarController;
    private boolean mNavigationLight;
    private float mScrimAlpha;
    private boolean mScrimAlphaBelowThreshold;
    public StatusBar mStatusBar;
    private final DarkIconDispatcher mStatusBarIconController = ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class));
    private int mSystemUiVisibility;

    public LightBarController() {
        this.mBatteryController.addCallback(this);
    }

    public void setFingerprintUnlockController(FingerprintUnlockController fingerprintUnlockController) {
        this.mFingerprintUnlockController = fingerprintUnlockController;
    }

    public void onSystemUiVisibilityChanged(int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds, boolean sbModeChanged, int statusBarMode) {
        int oldFullscreen = this.mFullscreenStackVisibility;
        int newFullscreen = ((~mask) & oldFullscreen) | (fullscreenStackVis & mask);
        int oldDocked = this.mDockedStackVisibility;
        int newDocked = ((~mask) & oldDocked) | (dockedStackVis & mask);
        int diffDocked = newDocked ^ oldDocked;
        if (((newFullscreen ^ oldFullscreen) & 8192) != 0 || (diffDocked & 8192) != 0 || sbModeChanged || !this.mLastFullscreenBounds.equals(fullscreenStackBounds) || !this.mLastDockedBounds.equals(dockedStackBounds)) {
            this.mFullscreenLight = isLight(newFullscreen, statusBarMode, 8192);
            this.mDockedLight = isLight(newDocked, statusBarMode, 8192);
            updateStatus(fullscreenStackBounds, dockedStackBounds);
        }
        this.mFullscreenStackVisibility = newFullscreen;
        this.mDockedStackVisibility = newDocked;
        this.mLastStatusBarMode = statusBarMode;
        this.mLastFullscreenBounds.set(fullscreenStackBounds);
        this.mLastDockedBounds.set(dockedStackBounds);
    }

    public void onNavigationVisibilityChanged(int vis, int mask, boolean nbModeChanged, int navigationBarMode) {
        int oldVis = this.mSystemUiVisibility;
        int newVis = ((~mask) & oldVis) | (vis & mask);
        if (((newVis ^ oldVis) & 16) != 0 || nbModeChanged) {
            boolean last = this.mNavigationLight;
            this.mHasLightNavigationBar = isLight(vis, navigationBarMode, 16);
            this.mNavigationLight = this.mHasLightNavigationBar && this.mScrimAlphaBelowThreshold;
            if (this.mNavigationLight != last) {
                updateNavigation();
            }
        }
        this.mSystemUiVisibility = newVis;
        this.mLastNavigationBarMode = navigationBarMode;
    }

    private void reevaluate() {
        onSystemUiVisibilityChanged(this.mFullscreenStackVisibility, this.mDockedStackVisibility, 0, this.mLastFullscreenBounds, this.mLastDockedBounds, true, this.mLastStatusBarMode);
        onNavigationVisibilityChanged(this.mSystemUiVisibility, 0, true, this.mLastNavigationBarMode);
    }

    public void setScrimAlpha(float alpha) {
        this.mScrimAlpha = alpha;
        boolean belowThresholdBefore = this.mScrimAlphaBelowThreshold;
        this.mScrimAlphaBelowThreshold = this.mScrimAlpha < 0.1f;
        if (this.mHasLightNavigationBar && belowThresholdBefore != this.mScrimAlphaBelowThreshold) {
            reevaluate();
        }
    }

    private boolean isLight(int vis, int barMode, int flag) {
        boolean allowLight = barMode == 4 || barMode == 6;
        boolean light = (vis & flag) != 0;
        if (!allowLight || !light) {
            return false;
        }
        return true;
    }

    private boolean animateChange() {
        boolean z = false;
        if (this.mFingerprintUnlockController == null) {
            return false;
        }
        int unlockMode = this.mFingerprintUnlockController.getMode();
        if (!(unlockMode == 2 || unlockMode == 1)) {
            z = true;
        }
        return z;
    }

    private void updateStatus(Rect fullscreenStackBounds, Rect dockedStackBounds) {
        this.mFullScreenStackBounds = fullscreenStackBounds;
        this.mDockedStackBounds = dockedStackBounds;
        boolean hasDockedStack = !dockedStackBounds.isEmpty();
        if (this.mDriveMode || this.mForceBlack) {
            this.mStatusBarIconController.getTransitionsController().setIconsDark(false, animateChange());
        } else if ((this.mFullscreenLight && this.mDockedLight) || (this.mFullscreenLight && !hasDockedStack)) {
            this.mStatusBarIconController.setIconsDarkArea(null);
            this.mStatusBarIconController.getTransitionsController().setIconsDark(true, animateChange());
            this.mStatusBar.setDarkMode(true);
        } else if ((this.mFullscreenLight || this.mDockedLight) && (this.mFullscreenLight || hasDockedStack)) {
            if (this.mFullscreenLight && fullscreenStackBounds.contains(dockedStackBounds)) {
                if (fullscreenStackBounds.bottom > dockedStackBounds.bottom) {
                    fullscreenStackBounds.top = dockedStackBounds.bottom;
                } else if (fullscreenStackBounds.left < dockedStackBounds.left) {
                    fullscreenStackBounds.right = dockedStackBounds.left;
                } else if (fullscreenStackBounds.right > dockedStackBounds.right) {
                    fullscreenStackBounds.left = dockedStackBounds.right;
                }
            }
            Rect bounds = this.mFullscreenLight ? fullscreenStackBounds : dockedStackBounds;
            if (bounds.isEmpty()) {
                this.mStatusBarIconController.setIconsDarkArea(null);
            } else {
                this.mStatusBarIconController.setIconsDarkArea(bounds);
            }
            this.mStatusBarIconController.getTransitionsController().setIconsDark(true, animateChange());
            this.mStatusBar.setDarkMode(true);
        } else {
            this.mStatusBarIconController.getTransitionsController().setIconsDark(false, animateChange());
            this.mStatusBar.setDarkMode(false);
        }
    }

    private void updateNavigation() {
        if (this.mNavigationBarController != null) {
            this.mNavigationBarController.setIconsDark(this.mNavigationLight, animateChange());
        }
    }

    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
    }

    public void onPowerSaveChanged(boolean isPowerSave) {
    }

    public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
    }

    public void onBatteryStyleChanged(int batteryStyle) {
    }

    public void setDriveMode(boolean driveMode) {
        this.mDriveMode = driveMode;
        updateStatus(this.mFullScreenStackBounds, this.mDockedStackBounds);
    }

    public void setForceBlack(boolean forceBlack) {
        this.mForceBlack = forceBlack;
        updateStatus(this.mFullScreenStackBounds, this.mDockedStackBounds);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("LightBarController: ");
        pw.print(" mSystemUiVisibility=0x");
        pw.print(Integer.toHexString(this.mSystemUiVisibility));
        pw.print(" mFullscreenStackVisibility=0x");
        pw.print(Integer.toHexString(this.mFullscreenStackVisibility));
        pw.print(" mDockedStackVisibility=0x");
        pw.println(Integer.toHexString(this.mDockedStackVisibility));
        pw.print(" mFullscreenLight=");
        pw.print(this.mFullscreenLight);
        pw.print(" mDockedLight=");
        pw.println(this.mDockedLight);
        pw.print(" mLastFullscreenBounds=");
        pw.print(this.mLastFullscreenBounds);
        pw.print(" mLastDockedBounds=");
        pw.println(this.mLastDockedBounds);
        pw.print(" mNavigationLight=");
        pw.print(this.mNavigationLight);
        pw.print(" mHasLightNavigationBar=");
        pw.println(this.mHasLightNavigationBar);
        pw.print(" mLastStatusBarMode=");
        pw.print(this.mLastStatusBarMode);
        pw.print(" mLastNavigationBarMode=");
        pw.println(this.mLastNavigationBarMode);
        pw.print(" mScrimAlpha=");
        pw.print(this.mScrimAlpha);
        pw.print(" mScrimAlphaBelowThreshold=");
        pw.println(this.mScrimAlphaBelowThreshold);
        pw.println();
        pw.println(" StatusBarTransitionsController:");
        this.mStatusBarIconController.getTransitionsController().dump(fd, pw, args);
        pw.println();
        pw.println(" NavigationBarTransitionsController:");
        this.mNavigationBarController.dump(fd, pw, args);
        pw.println();
    }
}
