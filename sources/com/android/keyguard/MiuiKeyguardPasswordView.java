package com.android.keyguard;

import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerCompat;
import android.content.Context;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.BackButton;
import com.android.keyguard.EmergencyButton;
import com.android.keyguard.utils.PhoneUtils;
import com.android.systemui.R;

public abstract class MiuiKeyguardPasswordView extends LinearLayout implements BackButton.BackButtonCallback, EmergencyButton.EmergencyButtonCallback {
    protected BackButton mBackButton;
    protected KeyguardSecurityCallback mCallback;
    protected TextView mDeleteButton;
    protected EmergencyButton mEmergencyButton;
    protected EmergencyCarrierArea mEmergencyCarrierArea;
    protected MiuiKeyguardFaceUnlockView mFaceUnlockView;
    private float mFontScale;
    protected KeyguardBouncerMessageView mKeyguardBouncerMessageView;
    protected KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    protected LockPatternUtils mLockPatternUtils;
    private int mOrientation;
    protected UserManager mUm;
    protected Vibrator mVibrator;

    /* access modifiers changed from: protected */
    public abstract String getPromptReasonString(int i);

    /* access modifiers changed from: protected */
    public abstract void handleConfigurationFontScaleChanged();

    /* access modifiers changed from: protected */
    public abstract void handleConfigurationOrientationChanged();

    public MiuiKeyguardPasswordView(Context context) {
        super(context);
    }

    public MiuiKeyguardPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mUm = (UserManager) this.mContext.getSystemService("user");
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mEmergencyCarrierArea = (EmergencyCarrierArea) findViewById(R.id.keyguard_selector_fade_container);
        this.mEmergencyButton = (EmergencyButton) findViewById(R.id.emergency_call_button);
        this.mEmergencyButton.setCallback(this);
        this.mBackButton = (BackButton) findViewById(R.id.back_button);
        this.mBackButton.setCallback(this);
        this.mDeleteButton = (TextView) findViewById(R.id.delete_button);
        this.mKeyguardBouncerMessageView = (KeyguardBouncerMessageView) findViewById(R.id.keyguard_security_bouncer_message);
        this.mFaceUnlockView = (MiuiKeyguardFaceUnlockView) findViewById(R.id.miui_keyguard_face_unlock_view);
        this.mFaceUnlockView.setKeyguardFaceUnlockView(false);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float fontScale = newConfig.fontScale;
        if (this.mFontScale != fontScale) {
            handleConfigurationFontScaleChanged();
            this.mFontScale = fontScale;
        }
        int orientation = newConfig.orientation;
        if (this.mOrientation != orientation) {
            handleConfigurationOrientationChanged();
            this.mOrientation = orientation;
        }
    }

    /* access modifiers changed from: protected */
    public void switchUser(int targetId) {
        if (KeyguardUpdateMonitor.getCurrentUser() != targetId) {
            try {
                ActivityManagerNative.getDefault().switchUser(targetId);
            } catch (RemoteException e) {
                Log.e("MiuiKeyguardPasswordView", "switchUser failed", e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean allowUnlock(int userId) {
        if (userId != 0 && !KeyguardUpdateMonitor.getInstance(this.mContext).getStrongAuthTracker().hasOwnerUserAuthenticatedSinceBoot()) {
            setSwitchUserWrongMessage(R.string.input_password_after_boot_msg_must_enter_owner_space);
            handleWrongPassword();
            return false;
        } else if (userId != KeyguardUpdateMonitor.getCurrentUser() && MiuiKeyguardUtils.isGreenKidActive(this.mContext)) {
            setSwitchUserWrongMessage(R.string.input_password_after_boot_msg_can_not_switch_when_greenkid_active);
            handleWrongPassword();
            return false;
        } else if (userId != KeyguardUpdateMonitor.getCurrentUser() && PhoneUtils.isInCall(this.mContext)) {
            Log.d("miui_keyguard_password", "Can't switch user to " + userId + " when calling");
            setSwitchUserWrongMessage(R.string.input_password_after_boot_msg_can_not_switch_when_calling);
            handleWrongPassword();
            return false;
        } else if (userId == KeyguardUpdateMonitor.getCurrentUser() || userId == 0 || userId != getManagedProfileId(this.mUm, UserHandle.myUserId())) {
            return true;
        } else {
            Log.d("miui_keyguard_password", "Can't switch user to " + userId + " when managed profile id");
            handleWrongPassword();
            return false;
        }
    }

    private int getManagedProfileId(UserManager um, int parentUserId) {
        int[] profileIds = UserManagerCompat.getProfileIdsWithDisabled(um, parentUserId);
        if (profileIds != null && profileIds.length > 0) {
            for (int profileId : profileIds) {
                if (profileId != parentUserId) {
                    return profileId;
                }
            }
        }
        return -10000;
    }

    public void onBackButtonClicked() {
        if (this.mCallback != null) {
            this.mCallback.reset();
        }
    }

    public void onEmergencyButtonClickedWhenInCall() {
        if (this.mCallback != null) {
            this.mCallback.reset();
            this.mCallback.userActivity();
        }
    }

    private void setSwitchUserWrongMessage(int resId) {
        this.mKeyguardBouncerMessageView.showMessage(0, resId);
    }

    /* access modifiers changed from: protected */
    public long getRequiredStrongAuthTimeout() {
        return DevicePolicyManagerCompat.getRequiredStrongAuthTimeout((DevicePolicyManager) this.mContext.getSystemService("device_policy"), null, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: protected */
    public void handleWrongPassword() {
    }
}
