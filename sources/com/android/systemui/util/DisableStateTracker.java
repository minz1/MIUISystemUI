package com.android.systemui.util;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;

public class DisableStateTracker implements View.OnAttachStateChangeListener, CommandQueue.Callbacks {
    private boolean mDisabled;
    private final int mMask1;
    private final int mMask2;
    private View mView;

    public DisableStateTracker(int disableMask, int disable2Mask) {
        this.mMask1 = disableMask;
        this.mMask2 = disable2Mask;
    }

    public void onViewAttachedToWindow(View v) {
        this.mView = v;
        ((CommandQueue) SystemUI.getComponent(v.getContext(), CommandQueue.class)).addCallbacks(this);
    }

    public void onViewDetachedFromWindow(View v) {
        ((CommandQueue) SystemUI.getComponent(this.mView.getContext(), CommandQueue.class)).removeCallbacks(this);
        this.mView = null;
    }

    public void disable(int state1, int state2, boolean animate) {
        int i = 0;
        boolean disabled = ((this.mMask1 & state1) == 0 && (this.mMask2 & state2) == 0) ? false : true;
        if (disabled != this.mDisabled) {
            this.mDisabled = disabled;
            View view = this.mView;
            if (disabled) {
                i = 8;
            }
            view.setVisibility(i);
        }
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
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

    public void showPictureInPictureMenu() {
    }

    public void addQsTile(ComponentName tile) {
    }

    public void remQsTile(ComponentName tile) {
    }

    public void clickTile(ComponentName tile) {
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
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
}
