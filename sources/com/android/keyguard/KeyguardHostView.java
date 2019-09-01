package com.android.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.systemui.R;
import java.io.File;

public class KeyguardHostView extends FrameLayout implements KeyguardSecurityContainer.SecurityCallback {
    private AudioManager mAudioManager;
    private Runnable mCancelAction;
    private OnDismissAction mDismissAction;
    protected LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityContainer mSecurityContainer;
    private final KeyguardUpdateMonitorCallback mUpdateCallback;
    protected ViewMediatorCallback mViewMediatorCallback;

    public interface OnDismissAction {
        boolean onDismiss();
    }

    public KeyguardHostView(Context context) {
        this(context, null);
    }

    public KeyguardHostView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            public void onUserSwitchComplete(int userId) {
                KeyguardHostView.this.getSecurityContainer().showPrimarySecurityScreen(false);
            }
        };
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateCallback);
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.keyguardDoneDrawing();
        }
    }

    public void setOnDismissAction(OnDismissAction action, Runnable cancelAction) {
        if (this.mCancelAction != null) {
            this.mCancelAction.run();
            this.mCancelAction = null;
        }
        this.mDismissAction = action;
        this.mCancelAction = cancelAction;
    }

    public void cancelDismissAction() {
        setOnDismissAction(null, null);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mSecurityContainer = (KeyguardSecurityContainer) findViewById(R.id.keyguard_security_container);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mSecurityContainer.setSecurityCallback(this);
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPrimarySecurityScreen() {
        Log.d("KeyguardViewBase", "show()");
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPromptReason(int reason) {
        this.mSecurityContainer.showPromptReason(reason);
    }

    public void showMessage(String message, int color) {
        this.mSecurityContainer.showMessage("", message, color);
    }

    public void showMessage(String title, String message, int color) {
        this.mSecurityContainer.showMessage(title, message, color);
    }

    public void applyHintAnimation(long offset) {
        this.mSecurityContainer.applyHintAnimation(offset);
    }

    public boolean dismiss(int targetUserId) {
        return dismiss(false, targetUserId);
    }

    public boolean handleBackKey() {
        LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Wallpaper_Uncovered");
        return this.mSecurityContainer.onBackPressed();
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
        event.getText().add(this.mSecurityContainer.getCurrentSecurityModeContentDescription());
        return true;
    }

    /* access modifiers changed from: protected */
    public KeyguardSecurityContainer getSecurityContainer() {
        return this.mSecurityContainer;
    }

    public boolean dismiss(boolean authenticated, int targetUserId) {
        return this.mSecurityContainer.showNextSecurityScreenOrFinish(authenticated, targetUserId);
    }

    public void finish(boolean strongAuth, int targetUserId) {
        boolean deferKeyguardDone = false;
        if (this.mDismissAction != null) {
            deferKeyguardDone = this.mDismissAction.onDismiss();
            this.mDismissAction = null;
            this.mCancelAction = null;
        }
        if (this.mViewMediatorCallback == null) {
            return;
        }
        if (deferKeyguardDone) {
            this.mViewMediatorCallback.keyguardDonePending(strongAuth, targetUserId);
        } else {
            this.mViewMediatorCallback.keyguardDone(strongAuth, targetUserId);
        }
    }

    public void reset() {
        this.mViewMediatorCallback.resetKeyguard();
    }

    public void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean needsInput) {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.setNeedsInput(needsInput);
        }
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().setSecurityMode(securityMode);
        }
    }

    public void userActivity() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.userActivity();
        }
    }

    public void onPause() {
        Log.d("KeyguardViewBase", String.format("screen off, instance %s at %s", new Object[]{Integer.toHexString(hashCode()), Long.valueOf(SystemClock.uptimeMillis())}));
        this.mSecurityContainer.showPrimarySecurityScreen(true);
        this.mSecurityContainer.onPause();
        clearFocus();
    }

    public void onResume() {
        Log.d("KeyguardViewBase", "screen on, instance " + Integer.toHexString(hashCode()));
        this.mSecurityContainer.onResume(1);
        requestFocus();
    }

    public void startAppearAnimation() {
        this.mSecurityContainer.startAppearAnimation();
    }

    public void startDisappearAnimation(Runnable finishRunnable) {
        if (!this.mSecurityContainer.startDisappearAnimation(finishRunnable) && finishRunnable != null) {
            finishRunnable.run();
        }
    }

    public void cleanUp() {
        getSecurityContainer().onPause();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (interceptMediaKey(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x002c, code lost:
        if (com.android.keyguard.utils.PhoneUtils.isInCall(r7.mContext) == false) goto L_0x0030;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x002e, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean interceptMediaKey(android.view.KeyEvent r8) {
        /*
            r7 = this;
            int r0 = r8.getKeyCode()
            int r1 = r8.getAction()
            r2 = 0
            r3 = 222(0xde, float:3.11E-43)
            r4 = 130(0x82, float:1.82E-43)
            r5 = 79
            r6 = 1
            if (r1 != 0) goto L_0x0034
            if (r0 == r5) goto L_0x0030
            if (r0 == r4) goto L_0x0030
            r1 = 164(0xa4, float:2.3E-43)
            if (r0 == r1) goto L_0x002f
            if (r0 == r3) goto L_0x0030
            switch(r0) {
                case 24: goto L_0x002f;
                case 25: goto L_0x002f;
                default: goto L_0x001f;
            }
        L_0x001f:
            switch(r0) {
                case 85: goto L_0x0026;
                case 86: goto L_0x0030;
                case 87: goto L_0x0030;
                case 88: goto L_0x0030;
                case 89: goto L_0x0030;
                case 90: goto L_0x0030;
                case 91: goto L_0x0030;
                default: goto L_0x0022;
            }
        L_0x0022:
            switch(r0) {
                case 126: goto L_0x0026;
                case 127: goto L_0x0026;
                default: goto L_0x0025;
            }
        L_0x0025:
            goto L_0x004b
        L_0x0026:
            android.content.Context r1 = r7.mContext
            boolean r1 = com.android.keyguard.utils.PhoneUtils.isInCall(r1)
            if (r1 == 0) goto L_0x0030
            return r6
        L_0x002f:
            return r2
        L_0x0030:
            r7.handleMediaKeyEvent(r8)
            return r6
        L_0x0034:
            int r1 = r8.getAction()
            if (r1 != r6) goto L_0x004b
            if (r0 == r5) goto L_0x0047
            if (r0 == r4) goto L_0x0047
            if (r0 == r3) goto L_0x0047
            switch(r0) {
                case 85: goto L_0x0047;
                case 86: goto L_0x0047;
                case 87: goto L_0x0047;
                case 88: goto L_0x0047;
                case 89: goto L_0x0047;
                case 90: goto L_0x0047;
                case 91: goto L_0x0047;
                default: goto L_0x0043;
            }
        L_0x0043:
            switch(r0) {
                case 126: goto L_0x0047;
                case 127: goto L_0x0047;
                default: goto L_0x0046;
            }
        L_0x0046:
            goto L_0x004b
        L_0x0047:
            r7.handleMediaKeyEvent(r8)
            return r6
        L_0x004b:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardHostView.interceptMediaKey(android.view.KeyEvent):boolean");
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        synchronized (this) {
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            }
        }
        this.mAudioManager.dispatchMediaKeyEvent(keyEvent);
    }

    public void dispatchSystemUiVisibilityChanged(int visibility) {
        super.dispatchSystemUiVisibilityChanged(visibility);
        if (!(this.mContext instanceof Activity)) {
            setSystemUiVisibility(4194304);
        }
    }

    public boolean shouldEnableMenuKey() {
        return !getResources().getBoolean(R.bool.config_disableMenuKeyInLockScreen) || ActivityManager.isRunningInTestHarness() || new File("/data/local/enable_menu_key").exists();
    }

    public void setViewMediatorCallback(ViewMediatorCallback viewMediatorCallback) {
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mViewMediatorCallback.setNeedsInput(this.mSecurityContainer.needsInput());
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mSecurityContainer.setLockPatternUtils(utils);
    }

    public KeyguardSecurityModel.SecurityMode getSecurityMode() {
        return this.mSecurityContainer.getSecurityMode();
    }

    public KeyguardSecurityModel.SecurityMode getCurrentSecurityMode() {
        return this.mSecurityContainer.getCurrentSecurityMode();
    }
}
