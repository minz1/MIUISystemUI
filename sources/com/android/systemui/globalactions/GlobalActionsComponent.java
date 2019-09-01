package com.android.systemui.globalactions;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUI;
import com.android.systemui.plugins.GlobalActions;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.util.function.Supplier;

public class GlobalActionsComponent extends SystemUI implements GlobalActions.GlobalActionsManager, CommandQueue.Callbacks {
    private IStatusBarService mBarService;
    private ExtensionController.Extension<GlobalActions> mExtension;

    public void start() {
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(GlobalActions.class).withPlugin(GlobalActions.class).withDefault(new Supplier() {
            public final Object get() {
                return GlobalActionsComponent.lambda$start$0(GlobalActionsComponent.this);
            }
        }).build();
        ((CommandQueue) getComponent(this.mContext, CommandQueue.class)).addCallbacks(this);
        putComponent(GlobalActions.GlobalActionsManager.class, this);
    }

    public static /* synthetic */ GlobalActions lambda$start$0(GlobalActionsComponent globalActionsComponent) {
        return new GlobalActionsImpl(globalActionsComponent.mContext);
    }

    public void handleShowGlobalActionsMenu() {
        this.mExtension.get().showGlobalActions(this);
    }

    public void onGlobalActionsShown() {
        try {
            this.mBarService.onGlobalActionsShown();
        } catch (RemoteException e) {
        }
    }

    public void onGlobalActionsHidden() {
        try {
            this.mBarService.onGlobalActionsHidden();
        } catch (RemoteException e) {
        }
    }

    public void shutdown() {
        try {
            this.mBarService.shutdown();
        } catch (RemoteException e) {
        }
    }

    public void reboot(boolean safeMode) {
        try {
            this.mBarService.reboot(safeMode);
        } catch (RemoteException e) {
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
