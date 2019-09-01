package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.List;

public class EmergencyCryptkeeperText extends TextView {
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        public void onPhoneStateChanged(int phoneState) {
            EmergencyCryptkeeperText.this.update();
        }

        public void onRefreshCarrierInfo() {
            EmergencyCryptkeeperText.this.update();
        }
    };
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                EmergencyCryptkeeperText.this.update();
            }
        }
    };

    public EmergencyCryptkeeperText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(8);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
        getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        update();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mKeyguardUpdateMonitor != null) {
            this.mKeyguardUpdateMonitor.removeCallback(this.mCallback);
        }
        getContext().unregisterReceiver(this.mReceiver);
    }

    public void update() {
        int i = 0;
        boolean hasMobile = ConnectivityManager.from(this.mContext).isNetworkSupported(0);
        boolean z = true;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
            z = false;
        }
        boolean airplaneMode = z;
        if (!hasMobile || airplaneMode) {
            setText(null);
            setVisibility(8);
            return;
        }
        List<SubscriptionInfo> subs = this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        int N = subs.size();
        CharSequence displayText = null;
        boolean allSimsMissing = true;
        for (int i2 = 0; i2 < N; i2++) {
            IccCardConstants.State simState = this.mKeyguardUpdateMonitor.getSimState(subs.get(i2).getSimSlotIndex());
            CharSequence carrierName = subs.get(i2).getCarrierName();
            if (simState.iccCardExist() && !TextUtils.isEmpty(carrierName)) {
                allSimsMissing = false;
                displayText = carrierName;
            }
        }
        if (allSimsMissing) {
            if (N != 0) {
                displayText = subs.get(0).getCarrierName();
            } else {
                displayText = getContext().getText(17039877);
                Intent i3 = getContext().registerReceiver(null, new IntentFilter("android.provider.Telephony.SPN_STRINGS_UPDATED"));
                if (i3 != null) {
                    displayText = i3.getStringExtra("plmn");
                }
            }
        }
        setText(displayText);
        if (TextUtils.isEmpty(displayText)) {
            i = 8;
        }
        setVisibility(i);
    }
}
