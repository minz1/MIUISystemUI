package com.android.systemui.globalactions;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.view.ContextThemeWrapper;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUI;
import com.android.systemui.plugins.GlobalActions;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class GlobalActionsImpl implements GlobalActions, CommandQueue.Callbacks {
    private final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    private boolean mDisabled;
    private GlobalActionsDialog mGlobalActions;
    private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));

    public GlobalActionsImpl(Context context) {
        this.mContext = context;
        ((CommandQueue) SystemUI.getComponent(context, CommandQueue.class)).addCallbacks(this);
    }

    public void showGlobalActions(GlobalActions.GlobalActionsManager manager) {
        if (!this.mDisabled) {
            if (this.mGlobalActions == null) {
                this.mGlobalActions = new GlobalActionsDialog(new ContextThemeWrapper(this.mContext, 16974391), manager);
            }
            this.mGlobalActions.showDialog(this.mKeyguardMonitor.isShowing(), this.mDeviceProvisionedController.isDeviceProvisioned());
        }
    }

    public void disable(int state1, int state2, boolean animate) {
        boolean disabled = (state2 & 8) != 0;
        if (disabled != this.mDisabled) {
            this.mDisabled = disabled;
            if (disabled && this.mGlobalActions != null) {
                this.mGlobalActions.dismissDialog();
            }
        }
    }

    public int getVersion() {
        return -1;
    }

    public void onCreate(Context sysuiContext, Context pluginContext) {
    }

    public void onDestroy() {
        ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).removeCallbacks(this);
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
