package com.android.systemui.fingerprint;

import android.content.ComponentName;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;

public class FingerprintDialogImpl extends SystemUI implements CommandQueue.Callbacks {
    private boolean mDialogShowing;
    private FingerprintDialogView mDialogView;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    FingerprintDialogImpl.this.handleShowDialog((SomeArgs) msg.obj);
                    return;
                case 2:
                    FingerprintDialogImpl.this.handleFingerprintAuthenticated();
                    return;
                case 3:
                    FingerprintDialogImpl.this.handleFingerprintHelp((String) msg.obj);
                    return;
                case 4:
                    FingerprintDialogImpl.this.handleFingerprintError((String) msg.obj);
                    return;
                case 5:
                    FingerprintDialogImpl.this.handleHideDialog(((Boolean) msg.obj).booleanValue());
                    return;
                case 6:
                    FingerprintDialogImpl.this.handleButtonNegative();
                    return;
                case 7:
                    FingerprintDialogImpl.this.handleUserCanceled();
                    return;
                case 8:
                    FingerprintDialogImpl.this.handleButtonPositive();
                    return;
                case 9:
                    FingerprintDialogImpl.this.handleClearMessage();
                    return;
                default:
                    return;
            }
        }
    };
    private IBiometricPromptReceiver mReceiver;
    private WindowManager mWindowManager;

    public void start() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            ((CommandQueue) getComponent(CommandQueue.class)).addCallbacks(this);
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
            this.mDialogView = new FingerprintDialogView(this.mContext, this.mHandler);
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

    public void showFingerprintDialog(SomeArgs args) {
        Log.d("FingerprintDialogImpl", "showFingerprintDialog");
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(1, args).sendToTarget();
    }

    public void onFingerprintAuthenticated() {
        Log.d("FingerprintDialogImpl", "onFingerprintAuthenticated");
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    public void onFingerprintHelp(String message) {
        Log.d("FingerprintDialogImpl", "onFingerprintHelp: " + message);
        this.mHandler.obtainMessage(3, message).sendToTarget();
    }

    public void onFingerprintError(String error) {
        Log.d("FingerprintDialogImpl", "onFingerprintError: " + error);
        this.mHandler.obtainMessage(4, error).sendToTarget();
    }

    public void hideFingerprintDialog() {
        Log.d("FingerprintDialogImpl", "hideFingerprintDialog");
        this.mHandler.obtainMessage(5, false).sendToTarget();
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
    }

    /* access modifiers changed from: private */
    public void handleShowDialog(SomeArgs args) {
        Log.d("FingerprintDialogImpl", "handleShowDialog, isAnimatingAway: " + this.mDialogView.isAnimatingAway());
        if (this.mDialogView.isAnimatingAway()) {
            this.mDialogView.forceRemove();
        } else if (this.mDialogShowing) {
            Log.w("FingerprintDialogImpl", "Dialog already showing");
            return;
        }
        try {
            this.mReceiver = (IBiometricPromptReceiver) args.arg2;
            this.mDialogView.setBundle((Bundle) args.arg1);
            this.mWindowManager.addView(this.mDialogView, this.mDialogView.getLayoutParams());
            this.mDialogShowing = true;
        } catch (Exception e) {
            Log.e("", "");
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAuthenticated() {
        Log.d("FingerprintDialogImpl", "handleFingerprintAuthenticated");
        this.mDialogView.announceForAccessibility(this.mContext.getResources().getText(17039958));
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    public void handleFingerprintHelp(String message) {
        Log.d("FingerprintDialogImpl", "handleFingerprintHelp: " + message);
        this.mDialogView.showHelpMessage(message);
    }

    /* access modifiers changed from: private */
    public void handleFingerprintError(String error) {
        Log.d("FingerprintDialogImpl", "handleFingerprintError: " + error);
        if (!this.mDialogShowing) {
            Log.d("FingerprintDialogImpl", "Dialog already dismissed");
        } else {
            this.mDialogView.showErrorMessage(error);
        }
    }

    /* access modifiers changed from: private */
    public void handleHideDialog(boolean userCanceled) {
        Log.d("FingerprintDialogImpl", "handleHideDialog, userCanceled: " + userCanceled);
        if (!this.mDialogShowing) {
            Log.w("FingerprintDialogImpl", "Dialog already dismissed, userCanceled: " + userCanceled);
            return;
        }
        if (userCanceled) {
            try {
                this.mReceiver.onDialogDismissed(3);
            } catch (RemoteException e) {
                Log.e("FingerprintDialogImpl", "RemoteException when hiding dialog", e);
            }
        }
        this.mReceiver = null;
        this.mDialogShowing = false;
        this.mDialogView.startDismiss();
    }

    /* access modifiers changed from: private */
    public void handleButtonNegative() {
        if (this.mReceiver == null) {
            Log.e("FingerprintDialogImpl", "Receiver is null");
            return;
        }
        try {
            this.mReceiver.onDialogDismissed(2);
        } catch (RemoteException e) {
            Log.e("FingerprintDialogImpl", "Remote exception when handling negative button", e);
        }
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    public void handleButtonPositive() {
        if (this.mReceiver == null) {
            Log.e("FingerprintDialogImpl", "Receiver is null");
            return;
        }
        try {
            this.mReceiver.onDialogDismissed(1);
        } catch (RemoteException e) {
            Log.e("FingerprintDialogImpl", "Remote exception when handling positive button", e);
        }
        handleHideDialog(false);
    }

    /* access modifiers changed from: private */
    public void handleClearMessage() {
        this.mDialogView.resetMessage();
    }

    /* access modifiers changed from: private */
    public void handleUserCanceled() {
        handleHideDialog(true);
    }
}
