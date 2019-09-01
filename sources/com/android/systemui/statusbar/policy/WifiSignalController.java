package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import java.util.Objects;

public class WifiSignalController extends SignalController<WifiState, SignalController.IconGroup> {
    private final boolean mHasMobileData;
    /* access modifiers changed from: private */
    public final AsyncChannel mWifiChannel;
    private final WifiStatusTracker mWifiTracker;

    private class WifiHandler extends Handler {
        WifiHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                WifiSignalController.this.setActivity(msg.arg1);
            } else if (i == 69632) {
                if (msg.arg1 == 0) {
                    WifiSignalController.this.mWifiChannel.sendMessage(Message.obtain(this, 69633));
                } else {
                    Log.e(WifiSignalController.this.mTag, "Failed to connect to wifi");
                }
            }
        }
    }

    static class WifiState extends SignalController.State {
        boolean isTransient;
        boolean noNetwork;
        String ssid;
        String statusLabel;

        WifiState() {
        }

        public void copyFrom(SignalController.State s) {
            super.copyFrom(s);
            WifiState state = (WifiState) s;
            this.ssid = state.ssid;
            this.isTransient = state.isTransient;
            this.statusLabel = state.statusLabel;
            this.noNetwork = state.noNetwork;
        }

        /* access modifiers changed from: protected */
        public void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(",ssid=");
            builder.append(this.ssid);
            builder.append(",isTransient=");
            builder.append(this.isTransient);
            builder.append(",statusLabel=");
            builder.append(this.statusLabel);
            builder.append(",noNetwork=");
            builder.append(this.noNetwork);
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!super.equals(o)) {
                return false;
            }
            WifiState other = (WifiState) o;
            if (Objects.equals(other.ssid, this.ssid) && other.isTransient == this.isTransient && TextUtils.equals(other.statusLabel, this.statusLabel) && ((WifiState) o).noNetwork == this.noNetwork) {
                z = true;
            }
            return z;
        }
    }

    public WifiSignalController(Context context, boolean hasMobileData, CallbackHandler callbackHandler, NetworkControllerImpl networkController, NetworkScoreManager networkScoreManager) {
        this(context, hasMobileData, callbackHandler, networkController, (WifiManager) context.getSystemService("wifi"));
    }

    /* JADX WARNING: Illegal instructions before constructor call */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public WifiSignalController(android.content.Context r22, boolean r23, com.android.systemui.statusbar.policy.CallbackHandler r24, com.android.systemui.statusbar.policy.NetworkControllerImpl r25, android.net.wifi.WifiManager r26) {
        /*
            r21 = this;
            r6 = r21
            r7 = r22
            java.lang.String r1 = "WifiSignalController"
            r3 = 1
            r0 = r6
            r2 = r7
            r4 = r24
            r5 = r25
            r0.<init>(r1, r2, r3, r4, r5)
            java.lang.Class<android.net.NetworkScoreManager> r0 = android.net.NetworkScoreManager.class
            java.lang.Object r0 = r7.getSystemService(r0)
            android.net.NetworkScoreManager r0 = (android.net.NetworkScoreManager) r0
            java.lang.Class<android.net.ConnectivityManager> r1 = android.net.ConnectivityManager.class
            java.lang.Object r1 = r7.getSystemService(r1)
            android.net.ConnectivityManager r1 = (android.net.ConnectivityManager) r1
            com.android.settingslib.wifi.WifiStatusTracker r2 = new com.android.settingslib.wifi.WifiStatusTracker
            android.content.Context r9 = r6.mContext
            com.android.systemui.statusbar.policy.-$$Lambda$WifiSignalController$AffzGdHvQakHA4bIzi_tW1MVLCY r13 = new com.android.systemui.statusbar.policy.-$$Lambda$WifiSignalController$AffzGdHvQakHA4bIzi_tW1MVLCY
            r13.<init>()
            r8 = r2
            r10 = r26
            r11 = r0
            r12 = r1
            r8.<init>(r9, r10, r11, r12, r13)
            r6.mWifiTracker = r2
            com.android.settingslib.wifi.WifiStatusTracker r2 = r6.mWifiTracker
            r2.setListening(r3)
            r2 = r23
            r6.mHasMobileData = r2
            com.android.systemui.statusbar.policy.WifiSignalController$WifiHandler r3 = new com.android.systemui.statusbar.policy.WifiSignalController$WifiHandler
            android.os.Looper r4 = android.os.Looper.getMainLooper()
            r3.<init>(r4)
            com.android.internal.util.AsyncChannel r4 = new com.android.internal.util.AsyncChannel
            r4.<init>()
            r6.mWifiChannel = r4
            android.os.Messenger r4 = r26.getWifiServiceMessenger()
            if (r4 == 0) goto L_0x0057
            com.android.internal.util.AsyncChannel r5 = r6.mWifiChannel
            r5.connect(r7, r3, r4)
        L_0x0057:
            com.android.systemui.statusbar.policy.SignalController$State r5 = r6.mCurrentState
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r5 = (com.android.systemui.statusbar.policy.WifiSignalController.WifiState) r5
            com.android.systemui.statusbar.policy.SignalController$State r8 = r6.mLastState
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r8 = (com.android.systemui.statusbar.policy.WifiSignalController.WifiState) r8
            com.android.systemui.statusbar.policy.SignalController$IconGroup r15 = new com.android.systemui.statusbar.policy.SignalController$IconGroup
            java.lang.String r10 = "Wi-Fi Icons"
            int[][] r11 = com.android.systemui.statusbar.policy.WifiIcons.WIFI_SIGNAL_STRENGTH
            int[][] r12 = com.android.systemui.statusbar.policy.WifiIcons.QS_WIFI_SIGNAL_STRENGTH
            int[] r13 = com.android.systemui.statusbar.policy.AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH
            r14 = 2131233058(0x7f080922, float:1.8082243E38)
            r16 = 2131231862(0x7f080476, float:1.8079817E38)
            r17 = 2131233058(0x7f080922, float:1.8082243E38)
            r18 = 2131231862(0x7f080476, float:1.8079817E38)
            r19 = 2131820695(0x7f110097, float:1.9274112E38)
            r9 = r15
            r20 = r15
            r15 = r16
            r16 = r17
            r17 = r18
            r18 = r19
            r9.<init>(r10, r11, r12, r13, r14, r15, r16, r17, r18)
            r9 = r20
            r8.iconGroup = r9
            r5.iconGroup = r9
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.WifiSignalController.<init>(android.content.Context, boolean, com.android.systemui.statusbar.policy.CallbackHandler, com.android.systemui.statusbar.policy.NetworkControllerImpl, android.net.wifi.WifiManager):void");
    }

    /* access modifiers changed from: protected */
    public WifiState cleanState() {
        return new WifiState();
    }

    public void notifyListeners(NetworkController.SignalCallback callback) {
        boolean wifiVisible = ((WifiState) this.mCurrentState).enabled && ((WifiState) this.mCurrentState).connected;
        String wifiDesc = wifiVisible ? ((WifiState) this.mCurrentState).ssid : null;
        boolean ssidPresent = wifiVisible && ((WifiState) this.mCurrentState).ssid != null;
        String contentDescription = getStringIfExists(getContentDescription());
        if (((WifiState) this.mCurrentState).inetCondition == 0) {
            contentDescription = contentDescription + "," + this.mContext.getString(R.string.data_connection_no_internet);
        }
        String contentDescription2 = contentDescription;
        callback.setWifiIndicators(((WifiState) this.mCurrentState).enabled, new NetworkController.IconState(wifiVisible, getCurrentIconId(), contentDescription2), new NetworkController.IconState(((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), contentDescription2), ssidPresent && ((WifiState) this.mCurrentState).activityIn, ssidPresent && ((WifiState) this.mCurrentState).activityOut, wifiDesc, ((WifiState) this.mCurrentState).isTransient);
    }

    public int getCurrentIconId() {
        if (((WifiState) this.mCurrentState).noNetwork) {
            return R.drawable.stat_sys_wifi_signal_null;
        }
        return super.getCurrentIconId();
    }

    public void handleBroadcast(Intent intent) {
        this.mWifiTracker.handleBroadcast(intent);
        ((WifiState) this.mCurrentState).enabled = this.mWifiTracker.enabled;
        ((WifiState) this.mCurrentState).connected = this.mWifiTracker.connected;
        ((WifiState) this.mCurrentState).ssid = this.mWifiTracker.ssid;
        ((WifiState) this.mCurrentState).rssi = this.mWifiTracker.rssi;
        ((WifiState) this.mCurrentState).level = this.mWifiTracker.level;
        ((WifiState) this.mCurrentState).statusLabel = this.mWifiTracker.statusLabel;
        ((WifiState) this.mCurrentState).isTransient = this.mWifiTracker.state == 2 || this.mWifiTracker.state == 0;
        if (intent != null && "android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
            updateWifiNoNetwork();
        }
        notifyListenersIfNecessary();
    }

    /* access modifiers changed from: private */
    public void handleStatusUpdated() {
        ((WifiState) this.mCurrentState).statusLabel = this.mWifiTracker.statusLabel;
        notifyListenersIfNecessary();
    }

    public void updateWifiNoNetwork() {
        boolean noNetwork = false;
        NetworkInfo networkInfo = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
        if (networkInfo != null && ((WifiState) this.mCurrentState).connected && !ConnectivityManager.isNetworkTypeWifi(networkInfo.getType())) {
            noNetwork = true;
        }
        ((WifiState) this.mCurrentState).noNetwork = noNetwork;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setActivity(int wifiActivity) {
        boolean z = false;
        ((WifiState) this.mCurrentState).activityIn = wifiActivity == 3 || wifiActivity == 1;
        WifiState wifiState = (WifiState) this.mCurrentState;
        if (wifiActivity == 3 || wifiActivity == 2) {
            z = true;
        }
        wifiState.activityOut = z;
        notifyListenersIfNecessary();
    }
}
