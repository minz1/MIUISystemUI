package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.ConnectivityManagerCompat;
import android.net.NetworkCapabilities;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Constants;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.MCCUtils;
import com.android.systemui.R;
import com.android.systemui.VirtualSimUtils;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.CallStateController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.MobileSignalController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.WifiSignalController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import miui.os.Build;
import miui.telephony.SubscriptionInfo;
import miui.telephony.SubscriptionManager;
import miui.util.ObjectReference;
import miui.util.ReflectionUtils;

public class NetworkControllerImpl extends BroadcastReceiver implements DataUsageController.NetworkNameProvider, ConfigurationChangedReceiver, DemoMode, Dumpable, NetworkController, NetworkController.MobileTypeListener {
    static final boolean CHATTY = Log.isLoggable("NetworkControllerChat", 3);
    static final boolean DEBUG = Log.isLoggable("NetworkController", 3);
    private final AccessPointControllerImpl mAccessPoints;
    private boolean mAirplaneMode;
    /* access modifiers changed from: private */
    public final CallbackHandler mCallbackHandler;
    private Config mConfig;
    private final BitSet mConnectedTransports;
    /* access modifiers changed from: private */
    public final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private List<SubscriptionInfo> mCurrentSubscriptions;
    private int mCurrentUserId;
    private final DataSaverController mDataSaverController;
    private final DataUsageController mDataUsageController;
    private MobileSignalController mDefaultSignalController;
    private boolean mDemoInetCondition;
    private boolean mDemoMode;
    private WifiSignalController.WifiState mDemoWifiState;
    private int mEmergencySource;
    @VisibleForTesting
    final EthernetSignalController mEthernetSignalController;
    private final boolean mHasMobileDataFeature;
    private boolean mHasNoSims;
    private boolean mInetCondition;
    private boolean mIsEmergency;
    @VisibleForTesting
    ServiceState[] mLastServiceState;
    @VisibleForTesting
    boolean mListening;
    private Locale mLocale;
    @VisibleForTesting
    final SparseArray<MobileSignalController> mMobileSignalControllers;
    private String[] mMobileTypeList;
    private String[] mNetworkNameList;
    private String mNetworkNameSeparator;
    private final NetworkScoreManager mNetworkScoreManager;
    private final TelephonyManager mPhone;
    private int mPhoneCount;
    /* access modifiers changed from: private */
    public final Handler mReceiverHandler;
    private final Runnable mRegisterListeners;
    private boolean mShowPlmnSPn;
    private NetworkController.SignalState mSignalState;
    private final SubscriptionDefaults mSubDefaults;
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener;
    /* access modifiers changed from: private */
    public final SubscriptionManager mSubscriptionManager;
    private boolean mUserSetup;
    private final CurrentUserTracker mUserTracker;
    private final BitSet mValidatedTransports;
    /* access modifiers changed from: private */
    public final WifiManager mWifiManager;
    @VisibleForTesting
    final WifiSignalController mWifiSignalController;

    @VisibleForTesting
    static class Config {
        boolean alwaysShowCdmaRssi = false;
        boolean hideLtePlus = false;
        boolean hspaDataDistinguishable;
        boolean readIconsFromXml;
        boolean show4gForLte = false;
        boolean showAtLeast3G = false;
        boolean showRsrpSignalLevelforLTE;

        Config() {
        }

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources res = context.getResources();
            config.showAtLeast3G = res.getBoolean(R.bool.config_showMin3G);
            config.alwaysShowCdmaRssi = res.getBoolean(17956887);
            config.show4gForLte = res.getBoolean(R.bool.config_show4GForLTE);
            config.hspaDataDistinguishable = res.getBoolean(R.bool.config_hspa_data_distinguishable) && !Build.IS_CM_CUSTOMIZATION;
            config.hideLtePlus = res.getBoolean(R.bool.config_hideLtePlus);
            config.readIconsFromXml = res.getBoolean(R.bool.config_read_icons_from_xml);
            config.showRsrpSignalLevelforLTE = res.getBoolean(R.bool.config_showRsrpSignalLevelforLTE);
            return config;
        }
    }

    private class SubListener implements SubscriptionManager.OnSubscriptionsChangedListener {
        private SubListener() {
        }

        public void onSubscriptionsChanged() {
            NetworkControllerImpl.this.mReceiverHandler.post(new Runnable() {
                public void run() {
                    NetworkControllerImpl.this.updateMobileControllers();
                }
            });
        }
    }

    public class SubscriptionDefaults {
        public SubscriptionDefaults() {
        }

        public int getDefaultVoiceSubId() {
            return NetworkControllerImpl.this.mSubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        public int getDefaultDataSubId() {
            return NetworkControllerImpl.this.mSubscriptionManager.getDefaultDataSubscriptionId();
        }
    }

    /* JADX WARNING: Illegal instructions before constructor call */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NetworkControllerImpl(@com.miui.systemui.annotation.Inject android.content.Context r17, @com.miui.systemui.annotation.Inject(tag = "SysUiNetBg") android.os.Looper r18, @com.miui.systemui.annotation.Inject com.android.systemui.statusbar.policy.DeviceProvisionedController r19) {
        /*
            r16 = this;
            r14 = r16
            r15 = r17
            java.lang.String r0 = "connectivity"
            java.lang.Object r0 = r15.getSystemService(r0)
            r2 = r0
            android.net.ConnectivityManager r2 = (android.net.ConnectivityManager) r2
            java.lang.Class<android.net.NetworkScoreManager> r0 = android.net.NetworkScoreManager.class
            java.lang.Object r0 = r15.getSystemService(r0)
            r3 = r0
            android.net.NetworkScoreManager r3 = (android.net.NetworkScoreManager) r3
            java.lang.String r0 = "phone"
            java.lang.Object r0 = r15.getSystemService(r0)
            r4 = r0
            android.telephony.TelephonyManager r4 = (android.telephony.TelephonyManager) r4
            java.lang.String r0 = "wifi"
            java.lang.Object r0 = r15.getSystemService(r0)
            r5 = r0
            android.net.wifi.WifiManager r5 = (android.net.wifi.WifiManager) r5
            miui.telephony.SubscriptionManager r6 = miui.telephony.SubscriptionManager.getDefault()
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r7 = com.android.systemui.statusbar.policy.NetworkControllerImpl.Config.readConfig(r17)
            com.android.systemui.statusbar.policy.CallbackHandler r9 = new com.android.systemui.statusbar.policy.CallbackHandler
            r9.<init>()
            com.android.systemui.statusbar.policy.AccessPointControllerImpl r10 = new com.android.systemui.statusbar.policy.AccessPointControllerImpl
            r13 = r18
            r10.<init>(r15, r13)
            com.android.settingslib.net.DataUsageController r11 = new com.android.settingslib.net.DataUsageController
            r11.<init>(r15)
            r12 = 0
            r0 = r14
            r1 = r15
            r8 = r13
            r13 = r19
            r0.<init>(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13)
            android.os.Handler r0 = r14.mReceiverHandler
            java.lang.Runnable r1 = r14.mRegisterListeners
            r0.post(r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.NetworkControllerImpl.<init>(android.content.Context, android.os.Looper, com.android.systemui.statusbar.policy.DeviceProvisionedController):void");
    }

    @VisibleForTesting
    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager, NetworkScoreManager networkScoreManager, TelephonyManager telephonyManager, WifiManager wifiManager, SubscriptionManager subManager, Config config, Looper bgLooper, CallbackHandler callbackHandler, AccessPointControllerImpl accessPointController, DataUsageController dataUsageController, SubscriptionDefaults defaultsHandler, DeviceProvisionedController deviceProvisionedController) {
        Context context2 = context;
        final DeviceProvisionedController deviceProvisionedController2 = deviceProvisionedController;
        this.mMobileSignalControllers = new SparseArray<>();
        this.mConnectedTransports = new BitSet();
        this.mValidatedTransports = new BitSet();
        this.mAirplaneMode = false;
        this.mLocale = null;
        this.mCurrentSubscriptions = new ArrayList();
        this.mRegisterListeners = new Runnable() {
            public void run() {
                NetworkControllerImpl.this.registerListeners();
            }
        };
        this.mContext = context2;
        this.mConfig = config;
        this.mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        this.mNetworkNameList = new String[this.mPhoneCount];
        this.mMobileTypeList = new String[this.mPhoneCount];
        String string = context2.getString(R.string.status_bar_network_name_separator);
        this.mNetworkNameSeparator = string;
        this.mNetworkNameSeparator = string;
        this.mReceiverHandler = new Handler(bgLooper);
        this.mCallbackHandler = callbackHandler;
        this.mDataSaverController = new DataSaverControllerImpl(context2);
        this.mSubscriptionManager = subManager;
        this.mSubDefaults = new SubscriptionDefaults();
        this.mConnectivityManager = connectivityManager;
        this.mHasMobileDataFeature = this.mConnectivityManager.isNetworkSupported(0);
        this.mPhone = telephonyManager;
        this.mShowPlmnSPn = context.getResources().getBoolean(R.bool.show_plmn_and_spn_in_carrier);
        this.mWifiManager = wifiManager;
        this.mNetworkScoreManager = networkScoreManager;
        this.mLastServiceState = new ServiceState[this.mPhoneCount];
        this.mLocale = this.mContext.getResources().getConfiguration().locale;
        this.mAccessPoints = accessPointController;
        this.mDataUsageController = dataUsageController;
        this.mDataUsageController.setNetworkController(this);
        this.mDataUsageController.setCallback(new DataUsageController.Callback() {
            public void onMobileDataEnabled(boolean enabled) {
                NetworkControllerImpl.this.mCallbackHandler.setMobileDataEnabled(enabled);
            }
        });
        WifiSignalController wifiSignalController = r0;
        WifiSignalController wifiSignalController2 = new WifiSignalController(this.mContext, this.mHasMobileDataFeature, this.mCallbackHandler, this, this.mNetworkScoreManager);
        this.mWifiSignalController = wifiSignalController;
        this.mEthernetSignalController = new EthernetSignalController(this.mContext, this.mCallbackHandler, this);
        updateAirplaneMode(true);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int newUserId) {
                NetworkControllerImpl.this.onUserSwitched(newUserId);
            }
        };
        this.mUserTracker.startTracking();
        deviceProvisionedController2.addCallback(new DeviceProvisionedController.DeviceProvisionedListener() {
            public void onDeviceProvisionedChanged() {
            }

            public void onUserSwitched() {
                onUserSetupChanged();
            }

            public void onUserSetupChanged() {
                NetworkControllerImpl.this.setUserSetupComplete(deviceProvisionedController2.isUserSetup(deviceProvisionedController2.getCurrentUser()));
            }
        });
        this.mSignalState = new NetworkController.SignalState();
    }

    public DataSaverController getDataSaverController() {
        return this.mDataSaverController;
    }

    private boolean isCustomizationTest() {
        return Build.IS_CM_CUSTOMIZATION_TEST || Build.IS_CU_CUSTOMIZATION_TEST;
    }

    /* access modifiers changed from: private */
    public void registerListeners() {
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).registerListener();
        }
        if (this.mSubscriptionListener == null) {
            this.mSubscriptionListener = new SubListener();
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");
        for (int i2 = 1; i2 < this.mPhoneCount; i2++) {
            filter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED" + i2);
        }
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.ACTION_IMS_REGISTED");
        filter.addAction("android.intent.action.ACTION_SPEECH_CODEC_IS_HD");
        filter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        if (miui.telephony.TelephonyManager.getDefault().getCtVolteSupportedMode() > 0) {
            filter.addAction("miui.intent.action.ACTION_ENHANCED_4G_LTE_MODE_CHANGE_FOR_SLOT1");
            filter.addAction("miui.intent.action.ACTION_ENHANCED_4G_LTE_MODE_CHANGE_FOR_SLOT2");
        }
        this.mContext.registerReceiver(this, filter, null, this.mReceiverHandler);
        this.mListening = true;
        updateMobileControllers();
    }

    private void unregisterListeners() {
        this.mListening = false;
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            MobileSignalController mobileSignalController = this.mMobileSignalControllers.valueAt(i);
            mobileSignalController.unregisterListener();
            if (isCustomizationTest()) {
                mobileSignalController.setMobileTypeListener(null);
            }
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mSubscriptionListener);
        this.mContext.unregisterReceiver(this);
    }

    public NetworkController.AccessPointController getAccessPointController() {
        return this.mAccessPoints;
    }

    public DataUsageController getMobileDataController() {
        return this.mDataUsageController;
    }

    public void addEmergencyListener(NetworkController.EmergencyListener listener) {
        this.mCallbackHandler.setListening(listener, true);
        this.mCallbackHandler.setEmergencyCallsOnly(isEmergencyOnly());
    }

    public void removeEmergencyListener(NetworkController.EmergencyListener listener) {
        this.mCallbackHandler.setListening(listener, false);
    }

    public void addCarrierNameListener(NetworkController.CarrierNameListener listener) {
        this.mCallbackHandler.setListening(listener, true);
        for (int i = 0; i < this.mPhoneCount; i++) {
            this.mCallbackHandler.updateCarrierName(i, this.mNetworkNameList[i]);
        }
    }

    public void removeCarrierNameListener(NetworkController.CarrierNameListener listener) {
        this.mCallbackHandler.setListening(listener, false);
    }

    public void addMobileTypeListener(NetworkController.MobileTypeListener mobileTypeListener) {
        this.mCallbackHandler.setListening(mobileTypeListener, true);
        for (int i = 0; i < this.mPhoneCount; i++) {
            this.mCallbackHandler.updateMobileTypeName(i, this.mMobileTypeList[i]);
        }
    }

    public void removeMobileTypeListener(NetworkController.MobileTypeListener mobileTypeListener) {
        this.mCallbackHandler.setListening(mobileTypeListener, false);
    }

    public void updateMobileTypeName(int slotId, String mobileTypeName) {
        if (slotId >= this.mPhoneCount) {
            return;
        }
        if (this.mMobileTypeList[slotId] == null || !this.mMobileTypeList[slotId].equals(mobileTypeName)) {
            this.mMobileTypeList[slotId] = mobileTypeName;
            this.mCallbackHandler.updateMobileTypeName(slotId, mobileTypeName);
        }
    }

    public boolean hasMobileDataFeature() {
        return this.mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return this.mPhone.getPhoneType() != 0;
    }

    private MobileSignalController getDataController() {
        int dataSubId = this.mSubDefaults.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(dataSubId)) {
            if (DEBUG) {
                Log.e("NetworkController", "No data sim selected");
            }
            return this.mDefaultSignalController;
        } else if (this.mMobileSignalControllers.indexOfKey(dataSubId) >= 0) {
            return this.mMobileSignalControllers.get(dataSubId);
        } else {
            if (DEBUG) {
                Log.e("NetworkController", "Cannot find controller for data sub: " + dataSubId);
            }
            return this.mDefaultSignalController;
        }
    }

    public String getMobileDataNetworkName() {
        MobileSignalController controller = getDataController();
        return controller != null ? ((MobileSignalController.MobileState) controller.getState()).networkNameData : "";
    }

    public boolean isEmergencyOnly() {
        if (this.mMobileSignalControllers.size() == 0) {
            this.mEmergencySource = 0;
            boolean isEmergencyOnly = false;
            if (this.mLastServiceState != null) {
                boolean isEmergencyOnly2 = false;
                for (int i = 0; i < this.mPhoneCount; i++) {
                    isEmergencyOnly2 = isEmergencyOnly2 || (this.mLastServiceState[i] != null && this.mLastServiceState[i].isEmergencyOnly());
                }
                isEmergencyOnly = isEmergencyOnly2;
            }
            return isEmergencyOnly;
        }
        int voiceSubId = this.mSubDefaults.getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(voiceSubId)) {
            for (int i2 = 0; i2 < this.mMobileSignalControllers.size(); i2++) {
                MobileSignalController mobileSignalController = this.mMobileSignalControllers.valueAt(i2);
                if (!((MobileSignalController.MobileState) mobileSignalController.getState()).isEmergency) {
                    this.mEmergencySource = 100 + mobileSignalController.mSubscriptionInfo.getSubscriptionId();
                    if (DEBUG) {
                        Log.d("NetworkController", "Found emergency " + mobileSignalController.mTag);
                    }
                    return false;
                }
            }
        }
        if (this.mMobileSignalControllers.indexOfKey(voiceSubId) >= 0) {
            this.mEmergencySource = 200 + voiceSubId;
            if (DEBUG) {
                Log.d("NetworkController", "Getting emergency from " + voiceSubId);
            }
            return ((MobileSignalController.MobileState) this.mMobileSignalControllers.get(voiceSubId).getState()).isEmergency;
        } else if (this.mMobileSignalControllers.size() == 1) {
            this.mEmergencySource = 400 + this.mMobileSignalControllers.keyAt(0);
            if (DEBUG) {
                Log.d("NetworkController", "Getting assumed emergency from " + this.mMobileSignalControllers.keyAt(0));
            }
            return ((MobileSignalController.MobileState) this.mMobileSignalControllers.valueAt(0).getState()).isEmergency;
        } else {
            if (DEBUG) {
                Log.e("NetworkController", "Cannot find controller for voice sub: " + voiceSubId);
            }
            this.mEmergencySource = 300 + voiceSubId;
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public void recalculateEmergency() {
        this.mIsEmergency = isEmergencyOnly();
        this.mCallbackHandler.setEmergencyCallsOnly(this.mIsEmergency);
    }

    public void addCallback(NetworkController.SignalCallback cb) {
        cb.setSubs(this.mCurrentSubscriptions);
        cb.setIsAirplaneMode(new NetworkController.IconState(this.mAirplaneMode, (int) R.drawable.stat_sys_signal_flightmode, (int) R.string.accessibility_airplane_mode, this.mContext));
        cb.setNoSims(this.mHasNoSims);
        this.mWifiSignalController.notifyListeners(cb);
        this.mEthernetSignalController.notifyListeners(cb);
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).notifyListeners(cb);
        }
        this.mCallbackHandler.setListening(cb, true);
        this.mCallbackHandler.setSubs(this.mCurrentSubscriptions);
        this.mCallbackHandler.setIsAirplaneMode(new NetworkController.IconState(this.mAirplaneMode, (int) R.drawable.stat_sys_signal_flightmode, (int) R.string.accessibility_airplane_mode, this.mContext));
        this.mCallbackHandler.setNoSims(this.mHasNoSims);
    }

    public void removeCallback(NetworkController.SignalCallback cb) {
        this.mCallbackHandler.setListening(cb, false);
    }

    public void setWifiEnabled(final boolean enabled) {
        new AsyncTask<Void, Void, Void>() {
            /* access modifiers changed from: protected */
            public Void doInBackground(Void... args) {
                int wifiApState = NetworkControllerImpl.this.mWifiManager.getWifiApState();
                if (enabled && !NetworkControllerImpl.this.getWifiStaSapConcurrency(NetworkControllerImpl.this.mWifiManager) && (wifiApState == 12 || wifiApState == 13)) {
                    if (Build.VERSION.SDK_INT < 24) {
                        ConnectivityManagerCompat.stopTethering(NetworkControllerImpl.this.mWifiManager);
                    } else {
                        ConnectivityManagerCompat.stopTethering(NetworkControllerImpl.this.mConnectivityManager, 0);
                    }
                }
                NetworkControllerImpl.this.mWifiManager.setWifiEnabled(enabled);
                return null;
            }
        }.execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    public boolean getWifiStaSapConcurrency(WifiManager wifiManager) {
        if (Build.VERSION.SDK_INT > 27) {
            return true;
        }
        boolean z = false;
        ObjectReference<Boolean> reference = ReflectionUtils.tryCallMethod(wifiManager, "getWifiStaSapConcurrency", Boolean.class, new Object[0]);
        if (reference != null) {
            z = ((Boolean) reference.get()).booleanValue();
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void onUserSwitched(int newUserId) {
        this.mCurrentUserId = newUserId;
        this.mAccessPoints.onUserSwitched(newUserId);
        updateConnectivity();
    }

    public void onReceive(Context context, Intent intent) {
        String carrier;
        if (CHATTY) {
            Log.d("NetworkController", "onReceive: intent=" + intent);
        }
        int subId = intent.getIntExtra("subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        String action = intent.getAction();
        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE") || action.equals("android.net.conn.INET_CONDITION_ACTION")) {
            this.mWifiSignalController.updateWifiNoNetwork();
            updateConnectivity();
        } else {
            int i = 0;
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                refreshLocale();
                updateAirplaneMode(false);
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED")) {
                recalculateEmergency();
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                while (true) {
                    int i2 = i;
                    if (i2 >= this.mMobileSignalControllers.size()) {
                        break;
                    }
                    this.mMobileSignalControllers.valueAt(i2).handleBroadcast(intent);
                    i = i2 + 1;
                }
            } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                int max = TelephonyManager.getDefault().getPhoneCount();
                int iccCardCount = 0;
                for (int i3 = 0; i3 < max; i3++) {
                    if (TelephonyManager.getDefault().hasIccCard(i3)) {
                        iccCardCount++;
                    }
                }
                if (Constants.SUPPORT_DISABLE_USB_BY_SIM != 0 && iccCardCount > 0) {
                    Log.d("NetworkController", "has sim");
                    Settings.System.putInt(this.mContext.getContentResolver(), "disable_usb_by_sim", 0);
                }
                if (SubscriptionManager.isValidSubscriptionId(subId) && this.mMobileSignalControllers.indexOfKey(subId) >= 0) {
                    this.mMobileSignalControllers.get(subId).handleBroadcast(intent);
                }
                updateMobileControllers();
                String stateExtra = intent.getStringExtra("ss");
                if ("CARD_IO_ERROR".equals(stateExtra) || "ABSENT".equals(stateExtra)) {
                    this.mSignalState.speedHdMap.remove(Integer.valueOf(subId));
                    this.mSignalState.imsMap.remove(Integer.valueOf(subId));
                    this.mSignalState.vowifiMap.remove(Integer.valueOf(subId));
                }
            } else if (action.equals("android.intent.action.SERVICE_STATE")) {
                int slot = intent.getIntExtra("slot", SubscriptionManager.INVALID_SLOT_ID);
                if (slot != SubscriptionManager.INVALID_SLOT_ID) {
                    this.mLastServiceState[slot] = ServiceState.newFromBundle(intent.getExtras());
                    if (this.mMobileSignalControllers.size() == 0) {
                        recalculateEmergency();
                    }
                }
            } else if (isSpnUpdateActionSlot(action)) {
                int slot2 = intent.getIntExtra("slot", SubscriptionManager.INVALID_SLOT_ID);
                if (slot2 != SubscriptionManager.INVALID_SLOT_ID) {
                    TelephonyIcons.updateDataTypeMcc(this.mContext, this.mPhone.getSimOperatorNumericForPhone(slot2), slot2);
                    if (VirtualSimUtils.isVirtualSim(context, slot2)) {
                        carrier = VirtualSimUtils.getVirtualSimCarrierName(context);
                    } else {
                        carrier = getNetworkName(slot2, intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getStringExtra("spnData"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
                    }
                    if (slot2 < this.mPhoneCount) {
                        this.mNetworkNameList[slot2] = carrier;
                    }
                    if (!TextUtils.isEmpty(carrier)) {
                        this.mCallbackHandler.updateCarrierName(slot2, carrier);
                    }
                }
            } else if (action.equals("android.intent.action.ACTION_IMS_REGISTED")) {
                setImsRegister(subId, intent.getBooleanExtra("state", false));
                setVowifi(subId, intent.getBooleanExtra("wfc_state", false));
            } else if (action.equals("android.intent.action.ACTION_SPEECH_CODEC_IS_HD")) {
                setSpeechHd(subId, intent.getBooleanExtra("is_hd", false));
            } else if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                this.mWifiSignalController.handleBroadcast(intent);
            } else if (this.mMobileSignalControllers.indexOfKey(subId) >= 0) {
                this.mMobileSignalControllers.get(subId).handleBroadcast(intent);
            } else {
                updateMobileControllers();
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mConfig = Config.readConfig(this.mContext);
        this.mReceiverHandler.post(new Runnable() {
            public void run() {
                NetworkControllerImpl.this.handleConfigurationChanged();
            }
        });
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void handleConfigurationChanged() {
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).setConfiguration(this.mConfig);
        }
        refreshLocale();
    }

    /* access modifiers changed from: private */
    public void updateMobileControllers() {
        if (this.mListening) {
            doUpdateMobileControllers();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void doUpdateMobileControllers() {
        List<SubscriptionInfo> subscriptions = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            subscriptions = Collections.emptyList();
        }
        if (hasCorrectMobileControllers(subscriptions)) {
            updateNoSims();
            return;
        }
        setCurrentSubscriptions(subscriptions);
        updateNoSims();
        recalculateEmergency();
        if (isCustomizationTest()) {
            for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
                this.mMobileSignalControllers.valueAt(i).setMobileTypeListener(this);
            }
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void updateNoSims() {
        boolean hasNoSims = this.mHasMobileDataFeature && this.mMobileSignalControllers.size() == 0;
        if (hasNoSims != this.mHasNoSims) {
            this.mHasNoSims = hasNoSims;
            this.mCallbackHandler.setNoSims(this.mHasNoSims);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setCurrentSubscriptions(List<SubscriptionInfo> subscriptions) {
        int num;
        int num2;
        List<SubscriptionInfo> list = subscriptions;
        Collections.sort(list, new Comparator<SubscriptionInfo>() {
            public int compare(SubscriptionInfo lhs, SubscriptionInfo rhs) {
                if (lhs.getSlotId() == rhs.getSlotId()) {
                    return lhs.getSubscriptionId() - rhs.getSubscriptionId();
                }
                return lhs.getSlotId() - rhs.getSlotId();
            }
        });
        this.mCurrentSubscriptions = list;
        SparseArray sparseArray = new SparseArray();
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            sparseArray.put(this.mMobileSignalControllers.keyAt(i), this.mMobileSignalControllers.valueAt(i));
        }
        this.mMobileSignalControllers.clear();
        ArrayList arrayList = new ArrayList();
        int i2 = subscriptions.size();
        ((CallStateController) Dependency.get(CallStateController.class)).setSimCount(i2);
        int i3 = 0;
        while (true) {
            int i4 = i3;
            if (i4 >= i2) {
                break;
            }
            int subId = list.get(i4).getSubscriptionId();
            arrayList.add(Integer.valueOf(subId));
            if (sparseArray.indexOfKey(subId) >= 0) {
                this.mMobileSignalControllers.put(subId, (MobileSignalController) sparseArray.get(subId));
                sparseArray.remove(subId);
                num = i2;
                num2 = i4;
            } else {
                SubscriptionDefaults subscriptionDefaults = this.mSubDefaults;
                num = i2;
                num2 = i4;
                MobileSignalController controller = new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mCallbackHandler, this, list.get(i4), subscriptionDefaults, this.mReceiverHandler.getLooper());
                controller.setUserSetupComplete(this.mUserSetup);
                this.mMobileSignalControllers.put(subId, controller);
                if (list.get(num2).getSlotId() == 0) {
                    this.mDefaultSignalController = controller;
                }
                if (this.mListening) {
                    controller.registerListener();
                }
            }
            i3 = num2 + 1;
            i2 = num;
        }
        int num3 = i2;
        if (this.mListening) {
            int i5 = 0;
            while (true) {
                int i6 = i5;
                if (i6 >= sparseArray.size()) {
                    break;
                }
                int key = sparseArray.keyAt(i6);
                if (sparseArray.get(key) == this.mDefaultSignalController) {
                    this.mDefaultSignalController = null;
                }
                ((MobileSignalController) sparseArray.get(key)).unregisterListener();
                i5 = i6 + 1;
            }
        }
        this.mSignalState.updateMap(arrayList, this.mSignalState.imsMap);
        this.mSignalState.updateMap(arrayList, this.mSignalState.vowifiMap);
        this.mSignalState.updateMap(arrayList, this.mSignalState.speedHdMap);
        this.mCallbackHandler.setSubs(list);
        notifyAllListeners();
        pushConnectivityToSignals();
        updateAirplaneMode(true);
    }

    /* access modifiers changed from: private */
    public void setUserSetupComplete(final boolean userSetup) {
        this.mReceiverHandler.post(new Runnable() {
            public void run() {
                NetworkControllerImpl.this.handleSetUserSetupComplete(userSetup);
            }
        });
    }

    /* access modifiers changed from: private */
    public void handleSetUserSetupComplete(boolean userSetup) {
        this.mUserSetup = userSetup;
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).setUserSetupComplete(this.mUserSetup);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean hasCorrectMobileControllers(List<SubscriptionInfo> allSubscriptions) {
        if (allSubscriptions.size() != this.mMobileSignalControllers.size()) {
            return false;
        }
        for (SubscriptionInfo info : allSubscriptions) {
            if (this.mMobileSignalControllers.indexOfKey(info.getSubscriptionId()) < 0) {
                return false;
            }
        }
        return true;
    }

    private void updateAirplaneMode(boolean force) {
        int i = 0;
        boolean z = true;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
            z = false;
        }
        boolean airplaneMode = z;
        if (airplaneMode != this.mAirplaneMode || force) {
            this.mAirplaneMode = airplaneMode;
            while (true) {
                int i2 = i;
                if (i2 < this.mMobileSignalControllers.size()) {
                    this.mMobileSignalControllers.valueAt(i2).setAirplaneMode(this.mAirplaneMode);
                    i = i2 + 1;
                } else {
                    notifyListeners();
                    return;
                }
            }
        }
    }

    private void refreshLocale() {
        Locale current = this.mContext.getResources().getConfiguration().locale;
        if (!current.equals(this.mLocale)) {
            this.mLocale = current;
            notifyAllListeners();
        }
    }

    private void setImsRegister(int subId, boolean imsRegister) {
        if (SubscriptionManager.isValidSubscriptionId(subId) && this.mMobileSignalControllers.indexOfKey(subId) >= 0) {
            this.mSignalState.imsMap.put(Integer.valueOf(subId), Boolean.valueOf(imsRegister));
            this.mMobileSignalControllers.get(subId).setImsRegister(this.mCallbackHandler, imsRegister);
        }
    }

    private void setSpeechHd(int subId, boolean speechHd) {
        if (SubscriptionManager.isValidSubscriptionId(subId) && this.mMobileSignalControllers.indexOfKey(subId) >= 0) {
            this.mSignalState.speedHdMap.put(Integer.valueOf(subId), Boolean.valueOf(speechHd));
            this.mMobileSignalControllers.get(subId).setSpeechHd(this.mCallbackHandler, speechHd);
        }
    }

    private void setVowifi(int subId, boolean vowifi) {
        if (SubscriptionManager.isValidSubscriptionId(subId) && this.mMobileSignalControllers.indexOfKey(subId) >= 0) {
            this.mSignalState.vowifiMap.put(Integer.valueOf(subId), Boolean.valueOf(vowifi));
            this.mMobileSignalControllers.get(subId).setVowifi(this.mCallbackHandler, vowifi);
        }
    }

    private void notifyAllListeners() {
        notifyListeners();
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).notifyListeners();
        }
        this.mWifiSignalController.notifyListeners();
        this.mEthernetSignalController.notifyListeners();
    }

    private void notifyListeners() {
        this.mCallbackHandler.setIsAirplaneMode(new NetworkController.IconState(this.mAirplaneMode, (int) R.drawable.stat_sys_signal_flightmode, (int) R.string.accessibility_airplane_mode, this.mContext));
        this.mCallbackHandler.setNoSims(this.mHasNoSims);
    }

    private void updateConnectivity() {
        this.mConnectedTransports.clear();
        this.mValidatedTransports.clear();
        for (NetworkCapabilities nc : this.mConnectivityManager.getDefaultNetworkCapabilitiesForUser(this.mCurrentUserId)) {
            for (int transportType : nc.getTransportTypes()) {
                this.mConnectedTransports.set(transportType);
                if (nc.hasCapability(16)) {
                    this.mValidatedTransports.set(transportType);
                }
            }
        }
        if (CHATTY) {
            Log.d("NetworkController", "updateConnectivity: mConnectedTransports=" + this.mConnectedTransports);
            Log.d("NetworkController", "updateConnectivity: mValidatedTransports=" + this.mValidatedTransports);
        }
        this.mInetCondition = !this.mValidatedTransports.isEmpty();
        pushConnectivityToSignals();
    }

    private void pushConnectivityToSignals() {
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        }
        this.mWifiSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        this.mEthernetSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
    }

    private boolean isSpnUpdateActionSlot(String action) {
        if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
            return true;
        }
        for (int i = 1; i < this.mPhoneCount; i++) {
            if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED" + i)) {
                return true;
            }
        }
        return false;
    }

    private String getNetworkName(int slot, boolean showSpn, String spn, String dataSpn, boolean showPlmn, String plmn) {
        StringBuilder str = new StringBuilder();
        boolean something = false;
        boolean showPlmnSpn = this.mShowPlmnSPn;
        if (this.mPhone != null) {
            String operator = this.mPhone.getSimOperatorNumericForPhone(slot);
            MCCUtils.checkOperation(this.mContext, operator);
            showPlmnSpn = showPlmnSpn || MCCUtils.isShowPlmnAndSpn(this.mContext, operator);
        }
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if ((showPlmnSpn || !something) && showSpn && spn != null) {
            if (str.length() != 0) {
                str.append(this.mNetworkNameSeparator);
            }
            str.append(spn);
        }
        return str.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");
        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());
        pw.println("  - connectivity ------");
        pw.print("  mConnectedTransports=");
        pw.println(this.mConnectedTransports);
        pw.print("  mValidatedTransports=");
        pw.println(this.mValidatedTransports);
        pw.print("  mInetCondition=");
        pw.println(this.mInetCondition);
        pw.print("  mAirplaneMode=");
        pw.println(this.mAirplaneMode);
        pw.print("  mHasNoSims:");
        pw.println(this.mHasNoSims);
        pw.print("  mLocale=");
        pw.println(this.mLocale);
        pw.print("  mLastServiceState=");
        pw.println(this.mLastServiceState);
        pw.print("  mIsEmergency=");
        pw.println(this.mIsEmergency);
        pw.print("  mEmergencySource=");
        pw.println(emergencyToString(this.mEmergencySource));
        for (int i = 0; i < this.mMobileSignalControllers.size(); i++) {
            this.mMobileSignalControllers.valueAt(i).dump(pw);
        }
        this.mWifiSignalController.dump(pw);
        this.mEthernetSignalController.dump(pw);
        this.mAccessPoints.dump(pw);
    }

    private static final String emergencyToString(int emergencySource) {
        if (emergencySource > 300) {
            return "ASSUMED_VOICE_CONTROLLER(" + (emergencySource - 200) + ")";
        } else if (emergencySource > 300) {
            return "NO_SUB(" + (emergencySource - 300) + ")";
        } else if (emergencySource > 200) {
            return "VOICE_CONTROLLER(" + (emergencySource - 200) + ")";
        } else if (emergencySource > 100) {
            return "FIRST_CONTROLLER(" + (emergencySource - 100) + ")";
        } else if (emergencySource == 0) {
            return "NO_CONTROLLERS";
        } else {
            return "UNKNOWN_SOURCE";
        }
    }

    public void dispatchDemoCommand(final String command, final Bundle args) {
        this.mReceiverHandler.post(new Runnable() {
            public void run() {
                NetworkControllerImpl.this.handleDemoCommand(command, args);
            }
        });
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x039e  */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x03a3  */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x03a8  */
    /* JADX WARNING: Removed duplicated region for block: B:185:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0169  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0170  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x0176  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x017c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleDemoCommand(java.lang.String r23, android.os.Bundle r24) {
        /*
            r22 = this;
            r0 = r22
            r1 = r23
            r2 = r24
            boolean r3 = r0.mDemoMode
            r4 = 1
            if (r3 != 0) goto L_0x0039
            java.lang.String r3 = "enter"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0039
            boolean r3 = DEBUG
            if (r3 == 0) goto L_0x001e
            java.lang.String r3 = "NetworkController"
            java.lang.String r5 = "Entering demo mode"
            android.util.Log.d(r3, r5)
        L_0x001e:
            r22.unregisterListeners()
            r0.mDemoMode = r4
            boolean r3 = r0.mInetCondition
            r0.mDemoInetCondition = r3
            com.android.systemui.statusbar.policy.WifiSignalController r3 = r0.mWifiSignalController
            com.android.systemui.statusbar.policy.SignalController$State r3 = r3.getState()
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r3 = (com.android.systemui.statusbar.policy.WifiSignalController.WifiState) r3
            r0.mDemoWifiState = r3
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r3 = r0.mDemoWifiState
            java.lang.String r4 = "DemoMode"
            r3.ssid = r4
            goto L_0x03ec
        L_0x0039:
            boolean r3 = r0.mDemoMode
            r5 = 0
            if (r3 == 0) goto L_0x007f
            java.lang.String r3 = "exit"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x007f
            boolean r3 = DEBUG
            if (r3 == 0) goto L_0x0051
            java.lang.String r3 = "NetworkController"
            java.lang.String r4 = "Exiting demo mode"
            android.util.Log.d(r3, r4)
        L_0x0051:
            r0.mDemoMode = r5
            r22.updateMobileControllers()
        L_0x0057:
            r3 = r5
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r4 = r0.mMobileSignalControllers
            int r4 = r4.size()
            if (r3 >= r4) goto L_0x006e
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r4 = r0.mMobileSignalControllers
            java.lang.Object r4 = r4.valueAt(r3)
            com.android.systemui.statusbar.policy.MobileSignalController r4 = (com.android.systemui.statusbar.policy.MobileSignalController) r4
            r4.resetLastState()
            int r5 = r3 + 1
            goto L_0x0057
        L_0x006e:
            com.android.systemui.statusbar.policy.WifiSignalController r3 = r0.mWifiSignalController
            r3.resetLastState()
            android.os.Handler r3 = r0.mReceiverHandler
            java.lang.Runnable r4 = r0.mRegisterListeners
            r3.post(r4)
            r22.notifyAllListeners()
            goto L_0x03ec
        L_0x007f:
            boolean r3 = r0.mDemoMode
            if (r3 == 0) goto L_0x03ec
            java.lang.String r3 = "network"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x03ec
            java.lang.String r3 = "airplane"
            java.lang.String r3 = r2.getString(r3)
            if (r3 == 0) goto L_0x00ab
            java.lang.String r6 = "show"
            boolean r6 = r3.equals(r6)
            com.android.systemui.statusbar.policy.CallbackHandler r7 = r0.mCallbackHandler
            com.android.systemui.statusbar.policy.NetworkController$IconState r8 = new com.android.systemui.statusbar.policy.NetworkController$IconState
            r9 = 2131232962(0x7f0808c2, float:1.8082048E38)
            r10 = 2131820600(0x7f110038, float:1.927392E38)
            android.content.Context r11 = r0.mContext
            r8.<init>((boolean) r6, (int) r9, (int) r10, (android.content.Context) r11)
            r7.setIsAirplaneMode(r8)
        L_0x00ab:
            java.lang.String r6 = "fully"
            java.lang.String r6 = r2.getString(r6)
            if (r6 == 0) goto L_0x00ee
            boolean r7 = java.lang.Boolean.parseBoolean(r6)
            r0.mDemoInetCondition = r7
            java.util.BitSet r7 = new java.util.BitSet
            r7.<init>()
            boolean r8 = r0.mDemoInetCondition
            if (r8 == 0) goto L_0x00c9
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            int r8 = r8.mTransportType
            r7.set(r8)
        L_0x00c9:
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r8.updateConnectivity(r7, r7)
            r8 = r5
        L_0x00cf:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r9 = r0.mMobileSignalControllers
            int r9 = r9.size()
            if (r8 >= r9) goto L_0x00ee
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r9 = r0.mMobileSignalControllers
            java.lang.Object r9 = r9.valueAt(r8)
            com.android.systemui.statusbar.policy.MobileSignalController r9 = (com.android.systemui.statusbar.policy.MobileSignalController) r9
            boolean r10 = r0.mDemoInetCondition
            if (r10 == 0) goto L_0x00e8
            int r10 = r9.mTransportType
            r7.set(r10)
        L_0x00e8:
            r9.updateConnectivity(r7, r7)
            int r8 = r8 + 1
            goto L_0x00cf
        L_0x00ee:
            java.lang.String r7 = "wifi"
            java.lang.String r7 = r2.getString(r7)
            r9 = 100357129(0x5fb5409, float:2.3634796E-35)
            r10 = 110414(0x1af4e, float:1.54723E-40)
            r11 = 3365(0xd25, float:4.715E-42)
            r12 = 2
            if (r7 == 0) goto L_0x0194
            java.lang.String r14 = "show"
            boolean r14 = r7.equals(r14)
            java.lang.String r15 = "level"
            java.lang.String r15 = r2.getString(r15)
            if (r15 == 0) goto L_0x0134
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r13 = r0.mDemoWifiState
            java.lang.String r5 = "null"
            boolean r5 = r15.equals(r5)
            if (r5 == 0) goto L_0x0119
            r5 = -1
            goto L_0x0125
        L_0x0119:
            int r5 = java.lang.Integer.parseInt(r15)
            int r17 = com.android.systemui.statusbar.policy.WifiIcons.WIFI_LEVEL_COUNT
            int r8 = r17 + -1
            int r5 = java.lang.Math.min(r5, r8)
        L_0x0125:
            r13.level = r5
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r5 = r0.mDemoWifiState
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r8 = r0.mDemoWifiState
            int r8 = r8.level
            if (r8 < 0) goto L_0x0131
            r8 = r4
            goto L_0x0132
        L_0x0131:
            r8 = 0
        L_0x0132:
            r5.connected = r8
        L_0x0134:
            java.lang.String r5 = "activity"
            java.lang.String r5 = r2.getString(r5)
            if (r5 == 0) goto L_0x0185
            int r8 = r5.hashCode()
            if (r8 == r11) goto L_0x015b
            if (r8 == r10) goto L_0x0151
            if (r8 == r9) goto L_0x0147
            goto L_0x0165
        L_0x0147:
            java.lang.String r8 = "inout"
            boolean r8 = r5.equals(r8)
            if (r8 == 0) goto L_0x0165
            r8 = 0
            goto L_0x0166
        L_0x0151:
            java.lang.String r8 = "out"
            boolean r8 = r5.equals(r8)
            if (r8 == 0) goto L_0x0165
            r8 = r12
            goto L_0x0166
        L_0x015b:
            java.lang.String r8 = "in"
            boolean r8 = r5.equals(r8)
            if (r8 == 0) goto L_0x0165
            r8 = r4
            goto L_0x0166
        L_0x0165:
            r8 = -1
        L_0x0166:
            switch(r8) {
                case 0: goto L_0x017c;
                case 1: goto L_0x0176;
                case 2: goto L_0x0170;
                default: goto L_0x0169;
            }
        L_0x0169:
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r13 = 0
            r8.setActivity(r13)
            goto L_0x0184
        L_0x0170:
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r8.setActivity(r12)
            goto L_0x0183
        L_0x0176:
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r8.setActivity(r4)
            goto L_0x0183
        L_0x017c:
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r13 = 3
            r8.setActivity(r13)
        L_0x0183:
            r13 = 0
        L_0x0184:
            goto L_0x018b
        L_0x0185:
            r13 = 0
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r8.setActivity(r13)
        L_0x018b:
            com.android.systemui.statusbar.policy.WifiSignalController$WifiState r8 = r0.mDemoWifiState
            r8.enabled = r14
            com.android.systemui.statusbar.policy.WifiSignalController r8 = r0.mWifiSignalController
            r8.notifyListeners()
        L_0x0194:
            java.lang.String r5 = "sims"
            java.lang.String r5 = r2.getString(r5)
            r8 = 8
            if (r5 == 0) goto L_0x01db
            int r13 = java.lang.Integer.parseInt(r5)
            int r13 = android.util.MathUtils.constrain(r13, r4, r8)
            java.util.ArrayList r14 = new java.util.ArrayList
            r14.<init>()
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r15 = r0.mMobileSignalControllers
            int r15 = r15.size()
            if (r13 == r15) goto L_0x01db
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r15 = r0.mMobileSignalControllers
            r15.clear()
            miui.telephony.SubscriptionManager r15 = r0.mSubscriptionManager
            int r15 = r15.getSubscriptionInfoCount()
            r17 = r15
        L_0x01c0:
            r18 = r17
            int r12 = r15 + r13
            r9 = r18
            if (r9 >= r12) goto L_0x01d6
            miui.telephony.SubscriptionInfo r12 = r0.addSignalController(r9, r9)
            r14.add(r12)
            int r17 = r9 + 1
            r9 = 100357129(0x5fb5409, float:2.3634796E-35)
            r12 = 2
            goto L_0x01c0
        L_0x01d6:
            com.android.systemui.statusbar.policy.CallbackHandler r9 = r0.mCallbackHandler
            r9.setSubs(r14)
        L_0x01db:
            java.lang.String r9 = "nosim"
            java.lang.String r9 = r2.getString(r9)
            if (r9 == 0) goto L_0x01f2
            java.lang.String r12 = "show"
            boolean r12 = r9.equals(r12)
            r0.mHasNoSims = r12
            com.android.systemui.statusbar.policy.CallbackHandler r12 = r0.mCallbackHandler
            boolean r13 = r0.mHasNoSims
            r12.setNoSims(r13)
        L_0x01f2:
            java.lang.String r12 = "mobile"
            java.lang.String r12 = r2.getString(r12)
            if (r12 == 0) goto L_0x03c4
            java.lang.String r13 = "show"
            boolean r13 = r12.equals(r13)
            java.lang.String r14 = "datatype"
            java.lang.String r14 = r2.getString(r14)
            java.lang.String r15 = "slot"
            java.lang.String r15 = r2.getString(r15)
            boolean r17 = android.text.TextUtils.isEmpty(r15)
            if (r17 == 0) goto L_0x0215
            r17 = 0
            goto L_0x0219
        L_0x0215:
            int r17 = java.lang.Integer.parseInt(r15)
        L_0x0219:
            r19 = r17
            r10 = r19
            r11 = 0
            int r8 = android.util.MathUtils.constrain(r10, r11, r8)
            java.util.ArrayList r10 = new java.util.ArrayList
            r10.<init>()
        L_0x0227:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r11 = r0.mMobileSignalControllers
            int r11 = r11.size()
            if (r11 > r8) goto L_0x023f
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r11 = r0.mMobileSignalControllers
            int r11 = r11.size()
            miui.telephony.SubscriptionInfo r4 = r0.addSignalController(r11, r11)
            r10.add(r4)
            r4 = 1
            goto L_0x0227
        L_0x023f:
            boolean r4 = r10.isEmpty()
            if (r4 != 0) goto L_0x024a
            com.android.systemui.statusbar.policy.CallbackHandler r4 = r0.mCallbackHandler
            r4.setSubs(r10)
        L_0x024a:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r4 = r0.mMobileSignalControllers
            java.lang.Object r4 = r4.valueAt(r8)
            com.android.systemui.statusbar.policy.MobileSignalController r4 = (com.android.systemui.statusbar.policy.MobileSignalController) r4
            com.android.systemui.statusbar.policy.SignalController$State r11 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r11 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r11
            if (r14 == 0) goto L_0x025c
            r1 = 1
            goto L_0x025d
        L_0x025c:
            r1 = 0
        L_0x025d:
            r11.dataSim = r1
            com.android.systemui.statusbar.policy.SignalController$State r1 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            if (r14 == 0) goto L_0x0269
            r11 = 1
            goto L_0x026a
        L_0x0269:
            r11 = 0
        L_0x026a:
            r1.isDefault = r11
            com.android.systemui.statusbar.policy.SignalController$State r1 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            if (r14 == 0) goto L_0x0276
            r11 = 1
            goto L_0x0277
        L_0x0276:
            r11 = 0
        L_0x0277:
            r1.dataConnected = r11
            if (r14 == 0) goto L_0x02f4
            com.android.systemui.statusbar.policy.SignalController$State r1 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            java.lang.String r11 = "1x"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x028d
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.ONE_X
            goto L_0x02f2
        L_0x028d:
            java.lang.String r11 = "3g"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x0298
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.THREE_G
            goto L_0x02f2
        L_0x0298:
            java.lang.String r11 = "4g"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02a3
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.FOUR_G
            goto L_0x02f2
        L_0x02a3:
            java.lang.String r11 = "4g+"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02ae
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.FOUR_G_PLUS
            goto L_0x02f2
        L_0x02ae:
            java.lang.String r11 = "e"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02b9
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.E
            goto L_0x02f2
        L_0x02b9:
            java.lang.String r11 = "g"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02c4
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.G
            goto L_0x02f2
        L_0x02c4:
            java.lang.String r11 = "h"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02cf
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.H
            goto L_0x02f2
        L_0x02cf:
            java.lang.String r11 = "lte"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02da
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.LTE
            goto L_0x02f2
        L_0x02da:
            java.lang.String r11 = "lte+"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02e5
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.LTE_PLUS
            goto L_0x02f2
        L_0x02e5:
            java.lang.String r11 = "dis"
            boolean r11 = r14.equals(r11)
            if (r11 == 0) goto L_0x02f0
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.DATA_DISABLED
            goto L_0x02f2
        L_0x02f0:
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r11 = com.android.systemui.statusbar.policy.TelephonyIcons.UNKNOWN
        L_0x02f2:
            r1.iconGroup = r11
        L_0x02f4:
            java.lang.String r1 = "roam"
            boolean r1 = r2.containsKey(r1)
            if (r1 == 0) goto L_0x0313
            com.android.systemui.statusbar.policy.SignalController$State r1 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            java.lang.String r11 = "show"
            r20 = r3
            java.lang.String r3 = "roam"
            java.lang.String r3 = r2.getString(r3)
            boolean r3 = r11.equals(r3)
            r1.roaming = r3
            goto L_0x0315
        L_0x0313:
            r20 = r3
        L_0x0315:
            java.lang.String r1 = "level"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x0352
            com.android.systemui.statusbar.policy.SignalController$State r3 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            java.lang.String r11 = "null"
            boolean r11 = r1.equals(r11)
            if (r11 == 0) goto L_0x032f
            r21 = r1
            r1 = -1
            goto L_0x033a
        L_0x032f:
            int r11 = java.lang.Integer.parseInt(r1)
            r21 = r1
            r1 = 6
            int r1 = java.lang.Math.min(r11, r1)
        L_0x033a:
            r3.level = r1
            com.android.systemui.statusbar.policy.SignalController$State r1 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            com.android.systemui.statusbar.policy.SignalController$State r3 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            int r3 = r3.level
            if (r3 < 0) goto L_0x034e
            r3 = 1
            goto L_0x034f
        L_0x034e:
            r3 = 0
        L_0x034f:
            r1.connected = r3
            goto L_0x0354
        L_0x0352:
            r21 = r1
        L_0x0354:
            java.lang.String r1 = "activity"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x03b4
            com.android.systemui.statusbar.policy.SignalController$State r3 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            r11 = 1
            r3.dataConnected = r11
            int r3 = r1.hashCode()
            r11 = 3365(0xd25, float:4.715E-42)
            if (r3 == r11) goto L_0x038e
            r11 = 110414(0x1af4e, float:1.54723E-40)
            if (r3 == r11) goto L_0x0383
            r11 = 100357129(0x5fb5409, float:2.3634796E-35)
            if (r3 == r11) goto L_0x0378
            goto L_0x0399
        L_0x0378:
            java.lang.String r3 = "inout"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0399
            r16 = 0
            goto L_0x039b
        L_0x0383:
            java.lang.String r3 = "out"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0399
            r16 = 2
            goto L_0x039b
        L_0x038e:
            java.lang.String r3 = "in"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L_0x0399
            r16 = 1
            goto L_0x039b
        L_0x0399:
            r16 = -1
        L_0x039b:
            switch(r16) {
                case 0: goto L_0x03ad;
                case 1: goto L_0x03a8;
                case 2: goto L_0x03a3;
                default: goto L_0x039e;
            }
        L_0x039e:
            r3 = 0
            r4.setActivity(r3)
            goto L_0x03b3
        L_0x03a3:
            r3 = 2
            r4.setActivity(r3)
            goto L_0x03b2
        L_0x03a8:
            r3 = 1
            r4.setActivity(r3)
            goto L_0x03b2
        L_0x03ad:
            r3 = 3
            r4.setActivity(r3)
        L_0x03b2:
            r3 = 0
        L_0x03b3:
            goto L_0x03b8
        L_0x03b4:
            r3 = 0
            r4.setActivity(r3)
        L_0x03b8:
            com.android.systemui.statusbar.policy.SignalController$State r11 = r4.getState()
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r11 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r11
            r11.enabled = r13
            r4.notifyListeners()
            goto L_0x03c7
        L_0x03c4:
            r20 = r3
            r3 = 0
        L_0x03c7:
            java.lang.String r1 = "carriernetworkchange"
            java.lang.String r1 = r2.getString(r1)
            if (r1 == 0) goto L_0x03ec
            java.lang.String r4 = "show"
            boolean r4 = r1.equals(r4)
        L_0x03d6:
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r8 = r0.mMobileSignalControllers
            int r8 = r8.size()
            if (r3 >= r8) goto L_0x03ec
            android.util.SparseArray<com.android.systemui.statusbar.policy.MobileSignalController> r8 = r0.mMobileSignalControllers
            java.lang.Object r8 = r8.valueAt(r3)
            com.android.systemui.statusbar.policy.MobileSignalController r8 = (com.android.systemui.statusbar.policy.MobileSignalController) r8
            r8.setCarrierNetworkChangeMode(r4)
            int r3 = r3 + 1
            goto L_0x03d6
        L_0x03ec:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.NetworkControllerImpl.handleDemoCommand(java.lang.String, android.os.Bundle):void");
    }

    private SubscriptionInfo addSignalController(int id, int simSlotIndex) {
        SubscriptionInfo info = SubscriptionManager.getDefault().getSubscriptionInfoForSlot(simSlotIndex);
        MobileSignalController controller = new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mCallbackHandler, this, info, this.mSubDefaults, this.mReceiverHandler.getLooper());
        this.mMobileSignalControllers.put(id, controller);
        ((MobileSignalController.MobileState) controller.getState()).userSetup = true;
        return info;
    }

    public boolean hasEmergencyCryptKeeperText() {
        return EncryptionHelper.IS_DATA_ENCRYPTED;
    }

    public boolean isRadioOn() {
        return !this.mAirplaneMode;
    }

    public NetworkController.SignalState getSignalState() {
        return this.mSignalState;
    }
}
