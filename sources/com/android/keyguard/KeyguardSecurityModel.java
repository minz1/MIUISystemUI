package com.android.keyguard;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;

public class KeyguardSecurityModel {
    private final Context mContext;
    private final boolean mIsPukScreenAvailable = this.mContext.getResources().getBoolean(17956974);
    private LockPatternUtils mLockPatternUtils;

    public enum SecurityMode {
        Invalid,
        None,
        Pattern,
        Password,
        PIN,
        SimPin,
        SimPuk
    }

    KeyguardSecurityModel(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    /* access modifiers changed from: package-private */
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    /* access modifiers changed from: package-private */
    public SecurityMode getSecurityMode(int userId) {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if (this.mIsPukScreenAvailable && SubscriptionManager.isValidSubscriptionId(monitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED))) {
            return SecurityMode.SimPuk;
        }
        if (SubscriptionManager.isValidSubscriptionId(monitor.getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED))) {
            return SecurityMode.SimPin;
        }
        int security = this.mLockPatternUtils.getActivePasswordQuality(userId);
        Log.v("KeyguardSecurityModel", "getSecurityMode security=" + security);
        if (security == 0) {
            return SecurityMode.None;
        }
        if (security == 65536) {
            return SecurityMode.Pattern;
        }
        if (security == 131072 || security == 196608) {
            return SecurityMode.PIN;
        }
        if (security == 262144 || security == 327680 || security == 393216 || security == 524288) {
            return SecurityMode.Password;
        }
        throw new IllegalStateException("Unknown security quality:" + security);
    }
}
