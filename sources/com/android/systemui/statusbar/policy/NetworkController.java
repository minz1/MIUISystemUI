package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.DemoMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.telephony.SubscriptionInfo;

public interface NetworkController extends DemoMode, CallbackController<SignalCallback> {

    public interface AccessPointController {

        public interface AccessPointCallback {
            void onAccessPointsChanged(List<AccessPoint> list);

            void onSettingsActivityTriggered(Intent intent);
        }

        void addAccessPointCallback(AccessPointCallback accessPointCallback);

        boolean canConfigWifi();

        boolean connect(AccessPoint accessPoint);

        int getIcon(AccessPoint accessPoint);

        void removeAccessPointCallback(AccessPointCallback accessPointCallback);

        void scanForAccessPoints();

        void updateVerboseLoggingLevel();
    }

    public interface CarrierNameListener {
        void updateCarrierName(int i, String str);
    }

    public interface EmergencyListener {
        void setEmergencyCallsOnly(boolean z);
    }

    public static class IconState {
        public final String contentDescription;
        public final int icon;
        public final int iconOverlay;
        public final boolean visible;

        public IconState(boolean visible2, int icon2, int iconOverlay2, String contentDescription2) {
            this.visible = visible2;
            this.icon = icon2;
            this.iconOverlay = iconOverlay2;
            this.contentDescription = contentDescription2;
        }

        public IconState(boolean visible2, int icon2, String contentDescription2) {
            this(visible2, icon2, -1, contentDescription2);
        }

        public IconState(boolean visible2, int icon2, int contentDescription2, Context context) {
            this(visible2, icon2, context.getString(contentDescription2));
        }
    }

    public interface MobileTypeListener {
        void updateMobileTypeName(int i, String str);
    }

    public interface SignalCallback {
        void setEthernetIndicators(IconState iconState);

        void setIsAirplaneMode(IconState iconState);

        void setIsDefaultDataSim(int i, boolean z);

        void setIsImsRegisted(int i, boolean z);

        void setMobileDataEnabled(boolean z);

        void setMobileDataIndicators(IconState iconState, IconState iconState2, int i, int i2, boolean z, boolean z2, int i3, int i4, int i5, String str, String str2, boolean z3, int i6, boolean z4);

        void setNetworkNameVoice(int i, String str);

        void setNoSims(boolean z);

        void setSpeechHd(int i, boolean z);

        void setSubs(List<SubscriptionInfo> list);

        void setVolteNoService(int i, boolean z);

        void setVowifi(int i, boolean z);

        void setWifiIndicators(boolean z, IconState iconState, IconState iconState2, boolean z2, boolean z3, String str, boolean z4);
    }

    public static class SignalState {
        public Map<Integer, Boolean> imsMap = new HashMap();
        public Map<Integer, Boolean> speedHdMap = new HashMap();
        public Map<Integer, Boolean> vowifiMap = new HashMap();

        public void updateMap(ArrayList<Integer> validSubIdList, Map<Integer, Boolean> map) {
            if (validSubIdList != null && map != null) {
                Iterator iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    if (!validSubIdList.contains(iterator.next().getKey())) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    void addCallback(SignalCallback signalCallback);

    void addCarrierNameListener(CarrierNameListener carrierNameListener);

    void addEmergencyListener(EmergencyListener emergencyListener);

    void addMobileTypeListener(MobileTypeListener mobileTypeListener);

    AccessPointController getAccessPointController();

    DataSaverController getDataSaverController();

    DataUsageController getMobileDataController();

    SignalState getSignalState();

    boolean hasEmergencyCryptKeeperText();

    boolean hasMobileDataFeature();

    boolean hasVoiceCallingFeature();

    boolean isRadioOn();

    void removeCallback(SignalCallback signalCallback);

    void removeCarrierNameListener(CarrierNameListener carrierNameListener);

    void removeEmergencyListener(EmergencyListener emergencyListener);

    void removeMobileTypeListener(MobileTypeListener mobileTypeListener);

    void setWifiEnabled(boolean z);
}
