package com.android.systemui.pip;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.SystemUI;
import com.android.systemui.pip.tv.PipManager;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.CommandQueue;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class PipUI extends SystemUI implements CommandQueue.Callbacks {
    private BasePipManager mPipManager;
    private boolean mSupportsPip;

    public void start() {
        BasePipManager basePipManager;
        PackageManager pm = this.mContext.getPackageManager();
        this.mSupportsPip = pm.hasSystemFeature("android.software.picture_in_picture");
        if (this.mSupportsPip) {
            if (SystemServicesProxy.getInstance(this.mContext).isSystemUser(SystemServicesProxy.getInstance(this.mContext).getProcessUser())) {
                if (pm.hasSystemFeature("android.software.leanback_only")) {
                    basePipManager = PipManager.getInstance();
                } else {
                    basePipManager = com.android.systemui.pip.phone.PipManager.getInstance();
                }
                this.mPipManager = basePipManager;
                this.mPipManager.initialize(this.mContext);
                ((CommandQueue) getComponent(CommandQueue.class)).addCallbacks(this);
                return;
            }
            throw new IllegalStateException("Non-primary Pip component not currently supported.");
        }
    }

    public void showPictureInPictureMenu() {
        this.mPipManager.showPictureInPictureMenu();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mPipManager != null) {
            this.mPipManager.onConfigurationChanged(newConfig);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mPipManager != null) {
            this.mPipManager.dump(pw);
        }
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
    }

    public void appTransitionCancelled() {
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
    }

    public void appTransitionFinished() {
    }

    public void showAssistDisclosure() {
    }

    public void startAssist(Bundle args) {
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
}
