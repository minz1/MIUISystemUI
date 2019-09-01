package com.android.keyguard;

import android.app.ActivityManagerCompat;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.ServiceState;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.utils.PhoneUtils;
import com.android.systemui.R;
import miui.os.Build;

public class EmergencyButton extends Button {
    private static final Intent INTENT_EMERGENCY_DIAL = new Intent().setAction("com.android.phone.EmergencyDialer.DIAL").setPackage("com.android.phone").setFlags(343932928);
    private int mDownX;
    private int mDownY;
    /* access modifiers changed from: private */
    public final EmergencyAffordanceManager mEmergencyAffordanceManager;
    private EmergencyButtonCallback mEmergencyButtonCallback;
    private final boolean mEnableEmergencyCallWhileSimLocked;
    KeyguardUpdateMonitorCallback mInfoCallback;
    private final boolean mIsVoiceCapable;
    private LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public boolean mLongPressWasDragged;
    private PowerManager mPowerManager;

    public interface EmergencyButtonCallback {
        void onEmergencyButtonClickedWhenInCall();
    }

    public EmergencyButton(Context context) {
        this(context, null);
    }

    public EmergencyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            public void onPhoneStateChanged(int phoneState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            public void onServiceStateChanged(int subId, ServiceState state) {
                EmergencyButton.this.updateEmergencyCallButton();
            }
        };
        this.mIsVoiceCapable = context.getResources().getBoolean(17957077);
        this.mEnableEmergencyCallWhileSimLocked = this.mContext.getResources().getBoolean(17956973);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EmergencyButton.this.takeEmergencyCallAction();
            }
        });
        setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (EmergencyButton.this.mLongPressWasDragged || PhoneUtils.isInCall(EmergencyButton.this.mContext) || (!EmergencyButton.this.mEmergencyAffordanceManager.needsEmergencyAffordance() && !MiuiKeyguardUtils.isIndianRegion(EmergencyButton.this.mContext))) {
                    return false;
                }
                EmergencyButton.this.mEmergencyAffordanceManager.performEmergencyCall();
                return true;
            }
        });
        updateEmergencyCallButton();
    }

    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getActionMasked() == 0) {
            this.mDownX = x;
            this.mDownY = y;
            this.mLongPressWasDragged = false;
        } else {
            int xDiff = Math.abs(x - this.mDownX);
            int yDiff = Math.abs(y - this.mDownY);
            int touchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
            if (Math.abs(yDiff) > touchSlop || Math.abs(xDiff) > touchSlop) {
                this.mLongPressWasDragged = true;
            }
        }
        return super.onTouchEvent(event);
    }

    public boolean performLongClick() {
        return super.performLongClick();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateEmergencyCallButton();
    }

    public void takeEmergencyCallAction() {
        MetricsLogger.action(this.mContext, 200);
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        try {
            ActivityManagerCompat.stopSystemLockTaskMode();
        } catch (RemoteException e) {
            Slog.w("EmergencyButton", "Failed to stop app pinning");
        }
        if (PhoneUtils.isInCall(this.mContext)) {
            PhoneUtils.resumeCall(this.mContext);
            if (this.mEmergencyButtonCallback != null) {
                this.mEmergencyButtonCallback.onEmergencyButtonClickedWhenInCall();
                return;
            }
            return;
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).reportEmergencyCallAction(true);
        getContext().startActivityAsUser(INTENT_EMERGENCY_DIAL, ActivityOptions.makeCustomAnimation(getContext(), 0, 0).toBundle(), new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
    }

    public void updateEmergencyCallButton() {
        int textId;
        boolean visible = false;
        if (this.mIsVoiceCapable && isDeviceSupport()) {
            if (PhoneUtils.isInCall(this.mContext)) {
                visible = true;
            } else {
                boolean z = true;
                if (KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinVoiceSecure()) {
                    visible = this.mEnableEmergencyCallWhileSimLocked;
                } else {
                    visible = this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || this.mContext.getResources().getBoolean(R.bool.config_showEmergencyButton);
                }
                if (this.mContext.getResources().getBoolean(R.bool.kg_hide_emgcy_btn_when_oos)) {
                    KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
                    if (!visible || monitor.isOOS()) {
                        z = false;
                    }
                    visible = z;
                }
            }
        }
        if (visible) {
            setVisibility(0);
            if (PhoneUtils.isInCall(this.mContext)) {
                textId = 17040234;
            } else {
                textId = R.string.emergency_call_string;
            }
            setText(textId);
            return;
        }
        setVisibility(4);
    }

    public void setCallback(EmergencyButtonCallback callback) {
        this.mEmergencyButtonCallback = callback;
    }

    private boolean isDeviceSupport() {
        return !Build.IS_TABLET;
    }
}
