package com.android.systemui.statusbar.policy;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.telephony.SubscriptionInfo;

public class CallbackHandler extends Handler implements NetworkController.CarrierNameListener, NetworkController.EmergencyListener, NetworkController.MobileTypeListener, NetworkController.SignalCallback {
    private final ArrayList<NetworkController.CarrierNameListener> mCarrierNameListeners = new ArrayList<>();
    private final ArrayList<NetworkController.EmergencyListener> mEmergencyListeners = new ArrayList<>();
    private final ArrayList<NetworkController.MobileTypeListener> mMobileTypeListeners = new ArrayList<>();
    /* access modifiers changed from: private */
    public final ArrayList<NetworkController.SignalCallback> mSignalCallbacks = new ArrayList<>();

    public CallbackHandler() {
        super(Looper.getMainLooper());
    }

    @VisibleForTesting
    CallbackHandler(Looper looper) {
        super(looper);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                Iterator<NetworkController.EmergencyListener> it = this.mEmergencyListeners.iterator();
                while (it.hasNext()) {
                    it.next().setEmergencyCallsOnly(msg.arg1 != 0);
                }
                return;
            case 1:
                Iterator<NetworkController.SignalCallback> it2 = this.mSignalCallbacks.iterator();
                while (it2.hasNext()) {
                    it2.next().setSubs((List) msg.obj);
                }
                return;
            case 2:
                Iterator<NetworkController.SignalCallback> it3 = this.mSignalCallbacks.iterator();
                while (it3.hasNext()) {
                    it3.next().setNoSims(msg.arg1 != 0);
                }
                return;
            case 3:
                Iterator<NetworkController.SignalCallback> it4 = this.mSignalCallbacks.iterator();
                while (it4.hasNext()) {
                    it4.next().setEthernetIndicators((NetworkController.IconState) msg.obj);
                }
                return;
            case 4:
                Iterator<NetworkController.SignalCallback> it5 = this.mSignalCallbacks.iterator();
                while (it5.hasNext()) {
                    it5.next().setIsAirplaneMode((NetworkController.IconState) msg.obj);
                }
                return;
            case 5:
                Iterator<NetworkController.SignalCallback> it6 = this.mSignalCallbacks.iterator();
                while (it6.hasNext()) {
                    it6.next().setMobileDataEnabled(msg.arg1 != 0);
                }
                return;
            case 6:
                if (msg.arg1 != 0) {
                    this.mEmergencyListeners.add((NetworkController.EmergencyListener) msg.obj);
                    return;
                } else {
                    this.mEmergencyListeners.remove((NetworkController.EmergencyListener) msg.obj);
                    return;
                }
            case 7:
                if (msg.arg1 != 0) {
                    this.mSignalCallbacks.add((NetworkController.SignalCallback) msg.obj);
                    return;
                } else {
                    this.mSignalCallbacks.remove((NetworkController.SignalCallback) msg.obj);
                    return;
                }
            case 8:
                Iterator<NetworkController.SignalCallback> it7 = this.mSignalCallbacks.iterator();
                while (it7.hasNext()) {
                    it7.next().setIsImsRegisted(msg.arg1, msg.arg2 == 1);
                }
                return;
            case 9:
                Iterator<NetworkController.SignalCallback> it8 = this.mSignalCallbacks.iterator();
                while (it8.hasNext()) {
                    it8.next().setVolteNoService(msg.arg1, msg.arg2 == 1);
                }
                return;
            case 10:
                Iterator<NetworkController.SignalCallback> it9 = this.mSignalCallbacks.iterator();
                while (it9.hasNext()) {
                    it9.next().setSpeechHd(msg.arg1, msg.arg2 == 1);
                }
                return;
            case 11:
                Iterator<NetworkController.SignalCallback> it10 = this.mSignalCallbacks.iterator();
                while (it10.hasNext()) {
                    it10.next().setNetworkNameVoice(msg.arg1, (String) msg.obj);
                }
                return;
            case 12:
                Iterator<NetworkController.SignalCallback> it11 = this.mSignalCallbacks.iterator();
                while (it11.hasNext()) {
                    it11.next().setVowifi(msg.arg1, msg.arg2 == 1);
                }
                return;
            case 13:
                if (msg.arg1 != 0) {
                    this.mCarrierNameListeners.add((NetworkController.CarrierNameListener) msg.obj);
                    return;
                } else {
                    this.mCarrierNameListeners.remove((NetworkController.CarrierNameListener) msg.obj);
                    return;
                }
            case 14:
                Iterator<NetworkController.CarrierNameListener> it12 = this.mCarrierNameListeners.iterator();
                while (it12.hasNext()) {
                    it12.next().updateCarrierName(msg.arg1, (String) msg.obj);
                }
                return;
            case 15:
                Iterator<NetworkController.SignalCallback> it13 = this.mSignalCallbacks.iterator();
                while (it13.hasNext()) {
                    it13.next().setIsDefaultDataSim(msg.arg1, ((Boolean) msg.obj).booleanValue());
                }
                return;
            case 16:
                if (msg.arg1 != 0) {
                    this.mMobileTypeListeners.add((NetworkController.MobileTypeListener) msg.obj);
                    return;
                } else {
                    this.mMobileTypeListeners.remove((NetworkController.MobileTypeListener) msg.obj);
                    return;
                }
            case 17:
                Iterator<NetworkController.MobileTypeListener> it14 = this.mMobileTypeListeners.iterator();
                while (it14.hasNext()) {
                    it14.next().updateMobileTypeName(msg.arg1, (String) msg.obj);
                }
                return;
            default:
                return;
        }
    }

    public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description, boolean isTransient) {
        final boolean z = enabled;
        final NetworkController.IconState iconState = statusIcon;
        final NetworkController.IconState iconState2 = qsIcon;
        final boolean z2 = activityIn;
        final boolean z3 = activityOut;
        final String str = description;
        final boolean z4 = isTransient;
        AnonymousClass1 r0 = new Runnable() {
            public void run() {
                Iterator it = CallbackHandler.this.mSignalCallbacks.iterator();
                while (it.hasNext()) {
                    ((NetworkController.SignalCallback) it.next()).setWifiIndicators(z, iconState, iconState2, z2, z3, str, z4);
                }
            }
        };
        post(r0);
    }

    public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int qsType, boolean activityIn, boolean activityOut, int dataActivityId, int stackedDataIcon, int stackedVoiceIcon, String typeContentDescription, String description, boolean isWide, int subId, boolean roaming) {
        final NetworkController.IconState iconState = statusIcon;
        final NetworkController.IconState iconState2 = qsIcon;
        final int i = statusType;
        final int i2 = qsType;
        final boolean z = activityIn;
        final boolean z2 = activityOut;
        final int i3 = dataActivityId;
        final int i4 = stackedDataIcon;
        final int i5 = stackedVoiceIcon;
        final String str = typeContentDescription;
        final String str2 = description;
        final boolean z3 = isWide;
        final int i6 = subId;
        AnonymousClass2 r16 = r0;
        final boolean z4 = roaming;
        AnonymousClass2 r0 = new Runnable() {
            public void run() {
                for (Iterator it = CallbackHandler.this.mSignalCallbacks.iterator(); it.hasNext(); it = it) {
                    ((NetworkController.SignalCallback) it.next()).setMobileDataIndicators(iconState, iconState2, i, i2, z, z2, i3, i4, i5, str, str2, z3, i6, z4);
                }
            }
        };
        post(r16);
    }

    public void setSubs(List<SubscriptionInfo> subs) {
        obtainMessage(1, subs).sendToTarget();
    }

    public void setNoSims(boolean show) {
        obtainMessage(2, show, 0).sendToTarget();
    }

    public void setMobileDataEnabled(boolean enabled) {
        obtainMessage(5, enabled, 0).sendToTarget();
    }

    public void setEmergencyCallsOnly(boolean emergencyOnly) {
        obtainMessage(0, emergencyOnly, 0).sendToTarget();
    }

    public void updateCarrierName(int slotId, String carrierName) {
        obtainMessage(14, slotId, 0, carrierName).sendToTarget();
    }

    public void updateMobileTypeName(int slotId, String mobileTypeName) {
        obtainMessage(17, slotId, 0, mobileTypeName).sendToTarget();
    }

    public void setEthernetIndicators(NetworkController.IconState icon) {
        obtainMessage(3, icon).sendToTarget();
    }

    public void setIsAirplaneMode(NetworkController.IconState icon) {
        obtainMessage(4, icon).sendToTarget();
    }

    public void setListening(NetworkController.EmergencyListener listener, boolean listening) {
        obtainMessage(6, listening, 0, listener).sendToTarget();
    }

    public void setListening(NetworkController.CarrierNameListener listener, boolean listening) {
        obtainMessage(13, listening, 0, listener).sendToTarget();
    }

    public void setListening(NetworkController.MobileTypeListener listener, boolean listening) {
        obtainMessage(16, listening, 0, listener).sendToTarget();
    }

    public void setListening(NetworkController.SignalCallback listener, boolean listening) {
        obtainMessage(7, listening, 0, listener).sendToTarget();
    }

    public void setIsImsRegisted(int slot, boolean imsRegisted) {
        obtainMessage(8, slot, imsRegisted).sendToTarget();
    }

    public void setVolteNoService(int slot, boolean show) {
        obtainMessage(9, slot, show).sendToTarget();
    }

    public void setSpeechHd(int slot, boolean hd) {
        obtainMessage(10, slot, hd).sendToTarget();
    }

    public void setVowifi(int slot, boolean vowifi) {
        obtainMessage(12, slot, vowifi).sendToTarget();
    }

    public void setNetworkNameVoice(int slot, String networkNameVoice) {
        obtainMessage(11, slot, 0, networkNameVoice).sendToTarget();
    }

    public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
        obtainMessage(15, slot, 0, Boolean.valueOf(isDefaultDataSim)).sendToTarget();
    }
}
