package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.CallStateController;
import com.android.systemui.statusbar.NetworkTypeUtils;
import com.android.systemui.statusbar.phone.SignalDrawable;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SignalController;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import miui.os.Build;
import miui.telephony.SubscriptionInfo;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyManagerEx;
import miui.util.FeatureParser;

public class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
    private static final boolean SUPPORT_CA = FeatureParser.getBoolean("support_ca", false);
    private final int STATUS_BAR_STYLE_ANDROID_DEFAULT = 0;
    private final int STATUS_BAR_STYLE_CDMA_1X_COMBINED = 1;
    private final int STATUS_BAR_STYLE_DATA_VOICE = 3;
    private final int STATUS_BAR_STYLE_DEFAULT_DATA = 2;
    private NetworkControllerImpl.Config mConfig;
    private MobileIconGroup mDefaultIcons;
    private final NetworkControllerImpl.SubscriptionDefaults mDefaults;
    private boolean mEnableVolteForSlot;
    private boolean mIsCtSim;
    private boolean mIsFirstSimStateChange = true;
    private boolean mIsQcom = "qcom".equals(FeatureParser.getString("vendor"));
    private boolean mIsShowVoiceType;
    private List<String> mMccNncList;
    private NetworkController.MobileTypeListener mMobileTypeListener;
    private final String mNetworkNameDefault;
    final SparseArray<MobileIconGroup> mNetworkToIconLookup = new SparseArray<>();
    private final TelephonyManager mPhone;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    private Resources mRes;
    /* access modifiers changed from: private */
    public ServiceState mServiceState;
    /* access modifiers changed from: private */
    public SignalStrength mSignalStrength;
    private IccCardConstants.State mSimState = IccCardConstants.State.READY;
    /* access modifiers changed from: private */
    public int mSlotId;
    private int mStyle = 0;
    private int mSubId;
    final SubscriptionInfo mSubscriptionInfo;
    private boolean mSupportDualVolte = miui.telephony.TelephonyManager.getDefault().isDualVolteSupported();

    static class MobileIconGroup extends SignalController.IconGroup {
        final int mActivityId;
        final int mDataContentDescription;
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;
        final int mSingleSignalIcon;
        final int mStackedDataIcon;
        final int mStackedVoiceIcon;

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc, int dataContentDesc, int dataType, boolean isWide, int qsDataType) {
            this(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState, qsDiscState, discContentDesc, dataContentDesc, dataType, isWide, qsDataType, 0, 0, 0, 0);
        }

        /* JADX INFO: super call moved to the top of the method (can break code semantics) */
        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc, int dataContentDesc, int dataType, boolean isWide, int qsDataType, int singleSignalIcon, int stackedDataIcon, int stackedVoicelIcon, int activityId) {
            super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState, qsDiscState, discContentDesc);
            this.mDataContentDescription = dataContentDesc;
            this.mDataType = dataType;
            this.mIsWide = isWide;
            this.mQsDataType = qsDataType;
            this.mSingleSignalIcon = singleSignalIcon;
            this.mStackedDataIcon = stackedDataIcon;
            this.mStackedVoiceIcon = stackedVoicelIcon;
            this.mActivityId = activityId;
        }
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(int subId, Looper looper) {
            super(Integer.valueOf(subId), looper);
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            String str;
            if (SignalController.DEBUG) {
                String str2 = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onSignalStrengthsChanged signalStrength=");
                sb.append(signalStrength);
                if (signalStrength == null) {
                    str = "";
                } else {
                    str = " level=" + signalStrength.getLevel();
                }
                sb.append(str);
                Log.d(str2, sb.toString());
            }
            if (MobileSignalController.this.needUpdateNetwork(MobileSignalController.this.mSlotId, MobileSignalController.this.mServiceState)) {
                SignalStrength unused = MobileSignalController.this.mSignalStrength = signalStrength;
                MobileSignalController.this.updateTelephony();
            }
        }

        public void onServiceStateChanged(ServiceState state) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onServiceStateChanged voiceState=" + state.getVoiceRegState() + " dataState=" + state.getDataRegState());
                String str2 = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onServiceStateChanged :state=");
                sb.append(state != null ? Integer.valueOf(state.getState()) : state);
                sb.append(" VoiceNetworkType=");
                sb.append(state != null ? Integer.valueOf(state.getVoiceNetworkType()) : state);
                sb.append(" DataNetworkType=");
                sb.append(state != null ? Integer.valueOf(state.getDataNetworkType()) : state);
                Log.d(str2, sb.toString());
            }
            if (MobileSignalController.this.needUpdateNetwork(MobileSignalController.this.mSlotId, state)) {
                ServiceState unused = MobileSignalController.this.mServiceState = state;
                if (state != null) {
                    ((MobileState) MobileSignalController.this.mCurrentState).dataNetType = NetworkTypeUtils.getDataNetTypeFromServiceState(state.getDataNetworkType(), state);
                }
                MobileSignalController.this.updateTelephony();
            }
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            ((MobileState) MobileSignalController.this.mCurrentState).callState = state;
            ((CallStateController) Dependency.get(CallStateController.class)).setCallState(MobileSignalController.this.mSlotId, state);
            MobileSignalController.this.updateTelephony();
        }

        public void onDataConnectionStateChanged(int state, int networkType) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onDataConnectionStateChanged: state=" + state + " type=" + networkType);
            }
            ((MobileState) MobileSignalController.this.mCurrentState).dataState = state;
            MobileSignalController.this.updateTelephony();
        }

        public void onDataActivity(int direction) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onDataActivity: direction=" + direction);
            }
            MobileSignalController.this.setActivity(direction);
        }

        public void onCarrierNetworkChange(boolean active) {
            if (SignalController.DEBUG) {
                String str = MobileSignalController.this.mTag;
                Log.d(str, "onCarrierNetworkChange: active=" + active);
            }
            ((MobileState) MobileSignalController.this.mCurrentState).carrierNetworkChangeMode = active;
            MobileSignalController.this.updateTelephony();
        }
    }

    static class MobileState extends SignalController.State {
        boolean airplaneMode;
        int callState;
        boolean carrierNetworkChangeMode;
        int dataActivity;
        boolean dataConnected;
        int dataNetType;
        boolean dataSim;
        int dataState;
        boolean imsRegister;
        boolean isDefault;
        boolean isEmergency;
        String networkName;
        String networkNameData;
        String networkNameVoice;
        int phoneType;
        boolean roaming;
        boolean speedHd;
        boolean userSetup;
        int voiceLevel;
        boolean volteNoService;
        boolean vowifi;

        MobileState() {
        }

        public void copyFrom(SignalController.State s) {
            super.copyFrom(s);
            MobileState state = (MobileState) s;
            this.dataSim = state.dataSim;
            this.networkName = state.networkName;
            this.networkNameData = state.networkNameData;
            this.networkNameVoice = state.networkNameVoice;
            this.dataNetType = state.dataNetType;
            this.dataState = state.dataState;
            this.dataConnected = state.dataConnected;
            this.isDefault = state.isDefault;
            this.isEmergency = state.isEmergency;
            this.airplaneMode = state.airplaneMode;
            this.carrierNetworkChangeMode = state.carrierNetworkChangeMode;
            this.userSetup = state.userSetup;
            this.roaming = state.roaming;
            this.imsRegister = state.imsRegister;
            this.speedHd = state.speedHd;
            this.vowifi = state.vowifi;
            this.volteNoService = state.volteNoService;
            this.dataActivity = state.dataActivity;
            this.voiceLevel = state.voiceLevel;
            this.phoneType = state.phoneType;
            this.callState = state.callState;
        }

        /* access modifiers changed from: protected */
        public void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',');
            builder.append("dataSim=");
            builder.append(this.dataSim);
            builder.append(',');
            builder.append("networkName=");
            builder.append(this.networkName);
            builder.append(',');
            builder.append("networkNameData=");
            builder.append(this.networkNameData);
            builder.append(',');
            builder.append("networkNameVoice=");
            builder.append(this.networkNameVoice);
            builder.append(',');
            builder.append("dataNetType=");
            builder.append(this.dataNetType);
            builder.append(',');
            builder.append("dataState=");
            builder.append(this.dataState);
            builder.append(',');
            builder.append("dataConnected=");
            builder.append(this.dataConnected);
            builder.append(',');
            builder.append("roaming=");
            builder.append(this.roaming);
            builder.append(',');
            builder.append("imsRegister=");
            builder.append(this.imsRegister);
            builder.append(',');
            builder.append("speedHd=");
            builder.append(this.speedHd);
            builder.append(',');
            builder.append("vowifi=");
            builder.append(this.vowifi);
            builder.append(',');
            builder.append("volteNoService=");
            builder.append(this.volteNoService);
            builder.append(',');
            builder.append("isDefault=");
            builder.append(this.isDefault);
            builder.append(',');
            builder.append("isEmergency=");
            builder.append(this.isEmergency);
            builder.append(',');
            builder.append("airplaneMode=");
            builder.append(this.airplaneMode);
            builder.append(',');
            builder.append("carrierNetworkChangeMode=");
            builder.append(this.carrierNetworkChangeMode);
            builder.append(',');
            builder.append("userSetup=");
            builder.append(this.userSetup);
            builder.append(',');
            builder.append("voiceLevel=");
            builder.append(this.voiceLevel);
            builder.append(',');
            builder.append("phoneType=");
            builder.append(this.phoneType);
            builder.append(',');
            builder.append("callState=");
            builder.append(this.callState);
            builder.append(',');
            builder.append("dataActivity=");
            builder.append(this.dataActivity);
        }

        public boolean equals(Object o) {
            return super.equals(o) && Objects.equals(((MobileState) o).networkName, this.networkName) && Objects.equals(((MobileState) o).networkNameData, this.networkNameData) && Objects.equals(((MobileState) o).networkNameVoice, this.networkNameVoice) && ((MobileState) o).dataNetType == this.dataNetType && ((MobileState) o).dataState == this.dataState && ((MobileState) o).dataSim == this.dataSim && ((MobileState) o).dataConnected == this.dataConnected && ((MobileState) o).isEmergency == this.isEmergency && ((MobileState) o).airplaneMode == this.airplaneMode && ((MobileState) o).carrierNetworkChangeMode == this.carrierNetworkChangeMode && ((MobileState) o).userSetup == this.userSetup && ((MobileState) o).isDefault == this.isDefault && ((MobileState) o).roaming == this.roaming && ((MobileState) o).imsRegister == this.imsRegister && ((MobileState) o).speedHd == this.speedHd && ((MobileState) o).vowifi == this.vowifi && ((MobileState) o).volteNoService == this.volteNoService && ((MobileState) o).voiceLevel == this.voiceLevel && ((MobileState) o).dataActivity == this.dataActivity && ((MobileState) o).phoneType == this.phoneType && ((MobileState) o).callState == this.callState;
        }
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public MobileSignalController(Context context, NetworkControllerImpl.Config config, boolean hasMobileData, TelephonyManager phone, CallbackHandler callbackHandler, NetworkControllerImpl networkController, SubscriptionInfo info, NetworkControllerImpl.SubscriptionDefaults defaults, Looper receiverLooper) {
        super("MobileSignalController(" + info.getSubscriptionId() + ")", context, 0, callbackHandler, networkController);
        String networkName;
        NetworkControllerImpl.Config config2 = config;
        boolean z = hasMobileData;
        this.mRes = context.getResources();
        this.mConfig = config2;
        this.mPhone = phone;
        this.mDefaults = defaults;
        this.mSubscriptionInfo = info;
        this.mSlotId = getSimSlotIndex();
        this.mSubId = this.mSubscriptionInfo.getSubscriptionId();
        this.mPhoneStateListener = new MobilePhoneStateListener(this.mSubId, receiverLooper);
        this.mNetworkNameDefault = getStringIfExists(17040206);
        this.mMccNncList = Arrays.asList(this.mContext.getResources().getStringArray(285605910));
        if (config2.readIconsFromXml) {
            TelephonyIcons.readIconsFromXml(context);
            this.mDefaultIcons = !this.mConfig.showAtLeast3G ? TelephonyIcons.G : TelephonyIcons.THREE_G;
        } else {
            mapIconSets();
        }
        this.mStyle = context.getResources().getInteger(R.integer.status_bar_style);
        if (info.getDisplayName() != null) {
            networkName = info.getDisplayName().toString();
        } else {
            networkName = this.mNetworkNameDefault;
        }
        ((MobileState) this.mCurrentState).networkName = networkName;
        ((MobileState) this.mLastState).networkName = networkName;
        ((MobileState) this.mCurrentState).networkNameData = networkName;
        ((MobileState) this.mLastState).networkNameData = networkName;
        ((MobileState) this.mCurrentState).enabled = z;
        ((MobileState) this.mLastState).enabled = z;
        MobileIconGroup mobileIconGroup = this.mDefaultIcons;
        ((MobileState) this.mCurrentState).iconGroup = mobileIconGroup;
        ((MobileState) this.mLastState).iconGroup = mobileIconGroup;
        updateDataSim();
    }

    public void setConfiguration(NetworkControllerImpl.Config config) {
        this.mConfig = config;
        if (!config.readIconsFromXml) {
            mapIconSets();
        }
        updateTelephony();
    }

    public void setAirplaneMode(boolean airplaneMode) {
        ((MobileState) this.mCurrentState).airplaneMode = airplaneMode;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean userSetup) {
        ((MobileState) this.mCurrentState).userSetup = userSetup;
        notifyListenersIfNecessary();
    }

    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        boolean isValidated = validatedTransports.get(this.mTransportType);
        ((MobileState) this.mCurrentState).isDefault = connectedTransports.get(this.mTransportType);
        ((MobileState) this.mCurrentState).inetCondition = (isValidated || !((MobileState) this.mCurrentState).isDefault) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean carrierNetworkChangeMode) {
        ((MobileState) this.mCurrentState).carrierNetworkChangeMode = carrierNetworkChangeMode;
        updateTelephony();
    }

    public void registerListener() {
        this.mPhone.listen(this.mPhoneStateListener, 66017);
    }

    public void unregisterListener() {
        this.mPhone.listen(this.mPhoneStateListener, 0);
    }

    private void mapIconSets() {
        this.mNetworkToIconLookup.clear();
        this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(17, TelephonyIcons.THREE_G);
        if (!this.mConfig.showAtLeast3G) {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.UNKNOWN);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.E);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.ONE_X);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.ONE_X);
            this.mDefaultIcons = TelephonyIcons.G;
        } else {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.THREE_G);
            this.mDefaultIcons = TelephonyIcons.THREE_G;
        }
        MobileIconGroup hGroup = TelephonyIcons.THREE_G;
        if (this.mConfig.hspaDataDistinguishable) {
            hGroup = TelephonyIcons.H;
        }
        this.mNetworkToIconLookup.put(8, hGroup);
        this.mNetworkToIconLookup.put(9, hGroup);
        this.mNetworkToIconLookup.put(10, hGroup);
        this.mNetworkToIconLookup.put(15, hGroup);
        if (this.mConfig.show4gForLte) {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
            if (this.mConfig.hideLtePlus) {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G);
            } else {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G_PLUS);
            }
        } else {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
            if (this.mConfig.hideLtePlus) {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE);
            } else {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE_PLUS);
            }
        }
        this.mNetworkToIconLookup.put(18, TelephonyIcons.WFC);
    }

    private int getNumLevels() {
        return 6;
    }

    public int getCurrentIconId() {
        if (!hasService()) {
            return TelephonyIcons.getSignalNullIcon(this.mSlotId);
        }
        int level = ((MobileState) this.mCurrentState).level;
        int[] iArr = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[0];
        int i = 5;
        if (level <= 5) {
            i = level;
        }
        return iArr[i];
    }

    public int getQsCurrentIconId() {
        if (((MobileState) this.mCurrentState).airplaneMode) {
            return SignalDrawable.getAirplaneModeState(getNumLevels());
        }
        if (((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        }
        boolean z = false;
        if (((MobileState) this.mCurrentState).connected) {
            int i = ((MobileState) this.mCurrentState).level;
            int numLevels = getNumLevels();
            if (((MobileState) this.mCurrentState).inetCondition == 0) {
                z = true;
            }
            return SignalDrawable.getState(i, numLevels, z);
        } else if (((MobileState) this.mCurrentState).enabled) {
            return SignalDrawable.getEmptyState(getNumLevels());
        } else {
            return 0;
        }
    }

    public void notifyListeners(NetworkController.SignalCallback callback) {
        boolean z;
        String name;
        NetworkController.SignalCallback signalCallback = callback;
        if (this.mConfig.readIconsFromXml) {
            generateIconGroup();
        }
        MobileIconGroup icons = (MobileIconGroup) getIcons();
        String contentDescription = getStringIfExists(getContentDescription());
        String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
        boolean dataDisabled = ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.DATA_DISABLED && ((MobileState) this.mCurrentState).userSetup;
        boolean showDataIcon = ((MobileState) this.mCurrentState).dataConnected || dataDisabled;
        NetworkController.IconState statusIcon = new NetworkController.IconState(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).airplaneMode, getCurrentIconId(), contentDescription);
        int qsTypeIcon = 0;
        NetworkController.IconState qsIcon = null;
        String description = null;
        if (((MobileState) this.mCurrentState).dataSim) {
            qsTypeIcon = showDataIcon ? icons.mQsDataType : 0;
            qsIcon = new NetworkController.IconState(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).isEmergency, getQsCurrentIconId(), contentDescription);
            description = ((MobileState) this.mCurrentState).isEmergency ? null : ((MobileState) this.mCurrentState).networkName;
        }
        int qsTypeIcon2 = qsTypeIcon;
        NetworkController.IconState qsIcon2 = qsIcon;
        String description2 = description;
        boolean activityIn = ((MobileState) this.mCurrentState).dataConnected && !((MobileState) this.mCurrentState).carrierNetworkChangeMode && ((MobileState) this.mCurrentState).activityIn;
        boolean activityOut = ((MobileState) this.mCurrentState).dataConnected && !((MobileState) this.mCurrentState).carrierNetworkChangeMode && ((MobileState) this.mCurrentState).activityOut;
        boolean z2 = showDataIcon & (((MobileState) this.mCurrentState).isDefault || dataDisabled) & (this.mStyle == 0);
        int dataActivityId = ((MobileState) this.mCurrentState).dataConnected;
        signalCallback.setNetworkNameVoice(this.mSlotId, ((MobileState) this.mCurrentState).networkNameVoice);
        signalCallback.setIsDefaultDataSim(this.mSlotId, ((MobileState) this.mCurrentState).dataSim);
        String str = contentDescription;
        MobileIconGroup icons2 = icons;
        signalCallback.setMobileDataIndicators(statusIcon, qsIcon2, icons.mDataType, qsTypeIcon2, activityIn, activityOut, (int) dataActivityId, icons.mStackedDataIcon, icons.mStackedVoiceIcon, dataContentDescription, description2, icons.mIsWide, this.mSlotId, ((MobileState) this.mCurrentState).roaming);
        if (!this.mEnableVolteForSlot || ((MobileState) this.mCurrentState).imsRegister || this.mServiceState == null || this.mServiceState.getRilVoiceRadioTechnology() == 6 || !this.mIsCtSim || ((MobileState) this.mCurrentState).airplaneMode) {
            z = false;
            ((MobileState) this.mCurrentState).volteNoService = false;
        } else {
            ((MobileState) this.mCurrentState).volteNoService = true;
            z = false;
        }
        if (((MobileState) this.mCurrentState).volteNoService && !this.mSupportDualVolte && this.mSlotId != SubscriptionManager.getDefault().getDefaultDataSlotId()) {
            ((MobileState) this.mCurrentState).volteNoService = z;
        }
        callback.setVolteNoService(this.mSlotId, ((MobileState) this.mCurrentState).volteNoService);
        if (this.mMobileTypeListener != null) {
            if (this.mIsShowVoiceType) {
                name = ((MobileState) this.mCurrentState).networkNameVoice;
                MobileIconGroup mobileIconGroup = icons2;
            } else {
                name = TelephonyIcons.getNetworkTypeName(icons2.mDataType, this.mSlotId);
            }
            this.mMobileTypeListener.updateMobileTypeName(this.mSlotId, name);
            return;
        }
    }

    /* access modifiers changed from: protected */
    public MobileState cleanState() {
        return new MobileState();
    }

    private boolean hasService() {
        return hasService(this.mServiceState);
    }

    private boolean hasService(ServiceState serviceState) {
        boolean z = false;
        if (serviceState == null) {
            return false;
        }
        switch (serviceState.getVoiceRegState()) {
            case 1:
            case 2:
                if (serviceState.getDataRegState() == 0) {
                    z = true;
                }
                return z;
            case 3:
                return false;
            default:
                return true;
        }
    }

    private boolean isCdma() {
        return (this.mSignalStrength != null && !this.mSignalStrength.isGsm()) || ((MobileState) this.mCurrentState).phoneType == 2;
    }

    public boolean isEmergencyOnly() {
        return this.mServiceState != null && this.mServiceState.isEmergencyOnly();
    }

    private boolean isRoaming() {
        boolean z = false;
        if (isCarrierNetworkChangeActive()) {
            return false;
        }
        if (!isCdma() || this.mServiceState == null) {
            return TelephonyManagerEx.getDefault().isNetworkRoamingForSlot(this.mSlotId);
        }
        int iconMode = this.mServiceState.getCdmaEriIconMode();
        int cdmaEriIconIndex = this.mServiceState.getCdmaEriIconIndex();
        if (cdmaEriIconIndex >= 0 && cdmaEriIconIndex != 1 && (iconMode == 0 || iconMode == 1)) {
            z = true;
        }
        return z;
    }

    private boolean isCarrierNetworkChangeActive() {
        return ((MobileState) this.mCurrentState).carrierNetworkChangeMode;
    }

    private boolean isChinaTelecomSim(int slotId) {
        String operator = miui.telephony.TelephonyManager.getDefault().getSimOperatorForSlot(slotId);
        if (TextUtils.isEmpty(operator)) {
            return false;
        }
        if (operator.equals("46003") || operator.equals("46011") || operator.equals("46005") || operator.equals("45502") || operator.equals("45507")) {
            return true;
        }
        return false;
    }

    private void updateCtSim(Intent intent, int slotId) {
        String stateExtra = intent.getStringExtra("ss");
        if ("LOADED".equals(stateExtra)) {
            this.mIsCtSim = isChinaTelecomSim(slotId);
        } else if ("ABSENT".equals(stateExtra)) {
            this.mIsCtSim = false;
        }
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            updateDataSim();
            notifyListenersIfNecessary();
        } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
            updateCtSim(intent, this.mSlotId);
            if (this.mIsFirstSimStateChange) {
                this.mIsFirstSimStateChange = false;
                ((MobileState) this.mCurrentState).phoneType = miui.telephony.TelephonyManager.getDefault().getPhoneTypeForSlot(this.mSlotId);
            }
        } else if (action.equals("miui.intent.action.ACTION_ENHANCED_4G_LTE_MODE_CHANGE_FOR_SLOT1") || action.equals("miui.intent.action.ACTION_ENHANCED_4G_LTE_MODE_CHANGE_FOR_SLOT2")) {
            this.mEnableVolteForSlot = intent.getBooleanExtra("extra_is_enhanced_4g_lte_on", false);
            notifyListenersIfNecessary();
        } else if (action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
            ((MobileState) this.mCurrentState).phoneType = miui.telephony.TelephonyManager.getDefault().getPhoneTypeForSlot(this.mSlotId);
            notifyListenersIfNecessary();
        }
    }

    private void updateDataSim() {
        int defaultDataSub = this.mDefaults.getDefaultDataSubId();
        boolean z = true;
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
            MobileState mobileState = (MobileState) this.mCurrentState;
            if (defaultDataSub != this.mSubId) {
                z = false;
            }
            mobileState.dataSim = z;
            return;
        }
        ((MobileState) this.mCurrentState).dataSim = true;
    }

    /* access modifiers changed from: private */
    public final void updateTelephony() {
        if (DEBUG) {
            Log.d(this.mTag, "updateTelephony: hasService=" + hasService() + " ss=" + this.mSignalStrength);
        }
        boolean z = false;
        ((MobileState) this.mCurrentState).connected = hasService() && this.mSignalStrength != null;
        if (!(this.mSignalStrength == null || this.mServiceState == null)) {
            ((MobileState) this.mCurrentState).level = this.mSignalStrength.getLevel();
            if (this.mConfig.showRsrpSignalLevelforLTE) {
                int dataType = this.mServiceState.getDataNetworkType();
                if (dataType == 13 || dataType == 19) {
                    ((MobileState) this.mCurrentState).level = getAlternateLteLevel(this.mSignalStrength);
                }
            }
        }
        if (this.mNetworkToIconLookup.indexOfKey(((MobileState) this.mCurrentState).dataNetType) >= 0) {
            ((MobileState) this.mCurrentState).iconGroup = this.mNetworkToIconLookup.get(((MobileState) this.mCurrentState).dataNetType);
        } else {
            ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
        }
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (((MobileState) this.mCurrentState).connected && ((MobileState) this.mCurrentState).dataState == 2) {
            z = true;
        }
        mobileState.dataConnected = z;
        updateVoiceType(this.mSlotId, getVoiceNetworkType());
        ((MobileState) this.mCurrentState).roaming = isRoaming();
        if (isCarrierNetworkChangeActive()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isDataDisabled()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.DATA_DISABLED;
        }
        if (isEmergencyOnly() != ((MobileState) this.mCurrentState).isEmergency) {
            ((MobileState) this.mCurrentState).isEmergency = isEmergencyOnly();
            this.mNetworkController.recalculateEmergency();
        }
        if (((MobileState) this.mCurrentState).networkName == this.mNetworkNameDefault && this.mServiceState != null && !TextUtils.isEmpty(this.mServiceState.getOperatorAlphaShort())) {
            ((MobileState) this.mCurrentState).networkName = this.mServiceState.getOperatorAlphaShort();
        }
        if (this.mServiceState != null) {
            ((MobileState) this.mCurrentState).networkNameData = TelephonyManager.getNetworkTypeName(this.mServiceState.getDataNetworkType());
        }
        if (this.mConfig.readIconsFromXml) {
            ((MobileState) this.mCurrentState).voiceLevel = getVoiceSignalLevel();
        }
        notifyListenersIfNecessary();
    }

    private boolean isDataDisabled() {
        return !this.mPhone.getDataEnabled(this.mSubId);
    }

    /* access modifiers changed from: private */
    public boolean needUpdateNetwork(int slotId, ServiceState serviceState) {
        CallStateController callStateController = (CallStateController) Dependency.get(CallStateController.class);
        if (!callStateController.isMsim() || callStateController.getCallState(1 - slotId) == 0 || !this.mSupportDualVolte || !this.mIsQcom || hasService(serviceState)) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x01f0  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x01f2  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0247  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0251  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x027d  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0284  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x02b9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void generateIconGroup() {
        /*
            r45 = this;
            r7 = r45
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r0 = r0.level
            r1 = 5
            if (r0 < r1) goto L_0x000d
            r0 = r1
            goto L_0x0013
        L_0x000d:
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r0 = r0.level
        L_0x0013:
            r8 = r0
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r0 = r0.voiceLevel
            if (r0 < r1) goto L_0x001e
            r0 = r1
            goto L_0x0024
        L_0x001e:
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r0 = r0.voiceLevel
        L_0x0024:
            r9 = r0
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r0 = r0.inetCondition
            if (r0 < r1) goto L_0x002e
            goto L_0x0034
        L_0x002e:
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r1 = r0.inetCondition
        L_0x0034:
            r10 = r1
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            boolean r11 = r0.dataConnected
            boolean r12 = r45.isRoaming()
            int r13 = r45.getVoiceNetworkType()
            int r14 = r45.getDataNetworkType()
            int[] r15 = com.android.systemui.statusbar.policy.AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH
            int[] r0 = com.android.systemui.statusbar.policy.AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH
            r16 = 0
            r35 = r0[r16]
            r17 = 0
            r18 = 0
            int r0 = r7.mSlotId
            if (r0 < 0) goto L_0x02f1
            int r0 = r7.mSlotId
            android.telephony.TelephonyManager r1 = r7.mPhone
            int r1 = r1.getPhoneCount()
            if (r0 <= r1) goto L_0x006b
            r42 = r8
            r43 = r9
            r44 = r10
            r36 = r11
            goto L_0x02f9
        L_0x006b:
            boolean r0 = DEBUG
            if (r0 == 0) goto L_0x00fd
            java.lang.String r0 = r7.mTag
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "generateIconGroup slot:"
            r1.append(r2)
            int r2 = r7.mSlotId
            r1.append(r2)
            java.lang.String r2 = " style:"
            r1.append(r2)
            int r2 = r7.mStyle
            r1.append(r2)
            java.lang.String r2 = " connected:"
            r1.append(r2)
            com.android.systemui.statusbar.policy.SignalController$State r2 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r2 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r2
            boolean r2 = r2.connected
            r1.append(r2)
            java.lang.String r2 = " inetCondition:"
            r1.append(r2)
            r1.append(r10)
            java.lang.String r2 = " roaming:"
            r1.append(r2)
            r1.append(r12)
            java.lang.String r2 = " level:"
            r1.append(r2)
            r1.append(r8)
            java.lang.String r2 = " voiceLevel:"
            r1.append(r2)
            r1.append(r9)
            java.lang.String r2 = " dataConnected:"
            r1.append(r2)
            r1.append(r11)
            java.lang.String r2 = " dataActivity:"
            r1.append(r2)
            com.android.systemui.statusbar.policy.SignalController$State r2 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r2 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r2
            int r2 = r2.dataActivity
            r1.append(r2)
            java.lang.String r2 = " CS:"
            r1.append(r2)
            r1.append(r13)
            java.lang.String r2 = "/"
            r1.append(r2)
            java.lang.String r2 = android.telephony.TelephonyManager.getNetworkTypeName(r13)
            r1.append(r2)
            java.lang.String r2 = ", PS:"
            r1.append(r2)
            r1.append(r14)
            java.lang.String r2 = "/"
            r1.append(r2)
            java.lang.String r2 = android.telephony.TelephonyManager.getNetworkTypeName(r14)
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.d(r0, r1)
        L_0x00fd:
            android.telephony.ServiceState r0 = r7.mServiceState
            if (r0 == 0) goto L_0x0127
            java.lang.String r0 = r7.mTag
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "getDataNetTypeFromServiceState slotId="
            r1.append(r2)
            int r2 = r7.mSlotId
            r1.append(r2)
            java.lang.String r2 = "  isUsingCarrierAggregation="
            r1.append(r2)
            android.telephony.ServiceState r2 = r7.mServiceState
            boolean r2 = android.telephony.ServiceStateCompat.isUsingCarrierAggregation(r2)
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.d(r0, r1)
        L_0x0127:
            android.telephony.ServiceState r0 = r7.mServiceState
            int r6 = com.android.systemui.statusbar.NetworkTypeUtils.getDataNetTypeFromServiceState(r14, r0)
            r5 = 1
            if (r6 == 0) goto L_0x0142
            boolean r0 = r45.isCdma()
            if (r0 == 0) goto L_0x013f
            com.android.systemui.statusbar.policy.SignalController$State r0 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r0 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r0
            int r0 = r0.callState
            if (r0 == 0) goto L_0x013f
            goto L_0x0142
        L_0x013f:
            r0 = r16
            goto L_0x0143
        L_0x0142:
            r0 = r5
        L_0x0143:
            r7.mIsShowVoiceType = r0
            boolean r0 = r7.mIsShowVoiceType
            if (r0 == 0) goto L_0x014b
            r2 = r13
            goto L_0x014c
        L_0x014b:
            r2 = r6
        L_0x014c:
            int r0 = r7.mSlotId
            r7.updateVoiceType(r0, r13)
            int r1 = r7.mSlotId
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r0 = r7.mConfig
            boolean r3 = r0.showAtLeast3G
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r0 = r7.mConfig
            boolean r4 = r0.show4gForLte
            com.android.systemui.statusbar.policy.NetworkControllerImpl$Config r0 = r7.mConfig
            boolean r0 = r0.hspaDataDistinguishable
            r19 = r0
            r0 = r7
            r36 = r11
            r11 = r5
            r5 = r19
            r37 = r6
            r6 = r10
            r0.updateDataType(r1, r2, r3, r4, r5, r6)
            int r0 = r7.mSlotId
            int r0 = com.android.systemui.statusbar.policy.TelephonyIcons.getSignalStrengthIcon(r0, r10, r8, r12)
            boolean r1 = DEBUG
            if (r1 == 0) goto L_0x0191
            java.lang.String r1 = r7.mTag
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "singleSignalIcon:"
            r3.append(r4)
            java.lang.String r4 = r7.getResourceName(r0)
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.d(r1, r3)
        L_0x0191:
            com.android.systemui.statusbar.policy.SignalController$State r1 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r1 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r1
            boolean r1 = r1.dataConnected
            if (r1 == 0) goto L_0x01aa
            int r1 = r7.mSlotId
            if (r1 < 0) goto L_0x01aa
            int r1 = r7.mSlotId
            com.android.systemui.statusbar.policy.SignalController$State r3 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r3 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r3
            int r3 = r3.dataActivity
            int r16 = com.android.systemui.statusbar.policy.TelephonyIcons.getDataActivity(r1, r3)
        L_0x01aa:
            r1 = r16
            int r3 = com.android.systemui.statusbar.policy.TelephonyIcons.convertMobileStrengthIcon(r0)
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x01ce
            java.lang.String r4 = r7.mTag
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "unstackedSignalIcon:"
            r5.append(r6)
            java.lang.String r6 = r7.getResourceName(r3)
            r5.append(r6)
            java.lang.String r5 = r5.toString()
            android.util.Log.d(r4, r5)
        L_0x01ce:
            if (r0 == r3) goto L_0x01d3
            r17 = r0
            r0 = r3
        L_0x01d3:
            int r4 = r7.mStyle
            if (r4 != r11) goto L_0x01ec
            if (r12 != 0) goto L_0x01e4
            boolean r4 = r45.showDataAndVoice()
            if (r4 == 0) goto L_0x01e4
            int r4 = com.android.systemui.statusbar.policy.TelephonyIcons.getStackedVoiceIcon(r9)
            goto L_0x01ee
        L_0x01e4:
            if (r12 == 0) goto L_0x01ec
            if (r1 == 0) goto L_0x01ec
            int r0 = com.android.systemui.statusbar.policy.TelephonyIcons.getRoamingSignalIconId(r8, r10)
        L_0x01ec:
            r4 = r18
        L_0x01ee:
            if (r4 != 0) goto L_0x01f2
            r5 = 0
            goto L_0x01f4
        L_0x01f2:
            r5 = r17
        L_0x01f4:
            int r6 = r7.mSlotId
            int[] r6 = com.android.systemui.statusbar.policy.TelephonyIcons.getSignalStrengthDes(r6)
            int r11 = r7.mSlotId
            int r11 = com.android.systemui.statusbar.policy.TelephonyIcons.getSignalNullIcon(r11)
            boolean r15 = DEBUG
            if (r15 == 0) goto L_0x0247
            java.lang.String r15 = r7.mTag
            r38 = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r39 = r3
            java.lang.String r3 = "singleSignalIcon="
            r2.append(r3)
            java.lang.String r3 = r7.getResourceName(r0)
            r2.append(r3)
            java.lang.String r3 = " dataActivityId="
            r2.append(r3)
            java.lang.String r3 = r7.getResourceName(r1)
            r2.append(r3)
            java.lang.String r3 = " stackedDataIcon="
            r2.append(r3)
            java.lang.String r3 = r7.getResourceName(r5)
            r2.append(r3)
            java.lang.String r3 = " stackedVoiceIcon="
            r2.append(r3)
            java.lang.String r3 = r7.getResourceName(r4)
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r15, r2)
            goto L_0x024b
        L_0x0247:
            r38 = r2
            r39 = r3
        L_0x024b:
            r2 = 18
            r3 = r37
            if (r3 != r2) goto L_0x0259
            r2 = 6
            r15 = 6
            r16 = 6
            r17 = 2131820632(0x7f110058, float:1.9273984E38)
            goto L_0x0275
        L_0x0259:
            int r2 = r7.mSlotId
            int r2 = com.android.systemui.statusbar.policy.TelephonyIcons.getDataTypeIcon(r2)
            int r15 = r7.mSlotId
            int r15 = com.android.systemui.statusbar.policy.TelephonyIcons.getQSDataTypeIcon(r15)
            int[] r16 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r40 = r2
            int r2 = r7.mSlotId
            r16 = r16[r2]
            int r2 = r7.mSlotId
            int r17 = com.android.systemui.statusbar.policy.TelephonyIcons.getDataTypeDesc(r2)
            r2 = r40
        L_0x0275:
            r41 = r17
            r42 = r8
            boolean r8 = r7.mIsShowVoiceType
            if (r8 == 0) goto L_0x0280
            r8 = 0
            r16 = r8
        L_0x0280:
            boolean r8 = DEBUG
            if (r8 == 0) goto L_0x02b9
            java.lang.String r8 = r7.mTag
            r43 = r9
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            r44 = r10
            java.lang.String r10 = "updateDataNetType, dataTypeIcon="
            r9.append(r10)
            java.lang.String r10 = r7.getResourceName(r2)
            r9.append(r10)
            java.lang.String r10 = " qsDataTypeIcon="
            r9.append(r10)
            java.lang.String r10 = r7.getResourceName(r15)
            r9.append(r10)
            java.lang.String r10 = " dataContentDesc="
            r9.append(r10)
            r10 = r41
            r9.append(r10)
            java.lang.String r9 = r9.toString()
            android.util.Log.d(r8, r9)
            goto L_0x02bf
        L_0x02b9:
            r43 = r9
            r44 = r10
            r10 = r41
        L_0x02bf:
            com.android.systemui.statusbar.policy.SignalController$State r8 = r7.mCurrentState
            com.android.systemui.statusbar.policy.MobileSignalController$MobileState r8 = (com.android.systemui.statusbar.policy.MobileSignalController.MobileState) r8
            com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup r9 = new com.android.systemui.statusbar.policy.MobileSignalController$MobileIconGroup
            java.lang.String r18 = android.telephony.TelephonyManager.getNetworkTypeName(r3)
            r19 = 0
            r20 = 0
            r22 = 0
            r23 = 0
            r25 = 0
            r29 = 0
            r17 = r9
            r21 = r6
            r24 = r11
            r26 = r35
            r27 = r10
            r28 = r16
            r30 = r15
            r31 = r0
            r32 = r5
            r33 = r4
            r34 = r1
            r17.<init>(r18, r19, r20, r21, r22, r23, r24, r25, r26, r27, r28, r29, r30, r31, r32, r33, r34)
            r8.iconGroup = r9
            return
        L_0x02f1:
            r42 = r8
            r43 = r9
            r44 = r10
            r36 = r11
        L_0x02f9:
            java.lang.String r0 = r7.mTag
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "generateIconGroup invalid slotId:"
            r1.append(r2)
            int r2 = r7.mSlotId
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.e(r0, r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.MobileSignalController.generateIconGroup():void");
    }

    private int getSimSlotIndex() {
        int slotId = -1;
        if (this.mSubscriptionInfo != null) {
            slotId = this.mSubscriptionInfo.getSlotId();
        }
        if (DEBUG) {
            String str = this.mTag;
            Log.d(str, "getSimSlotIndex, slotId: " + slotId);
        }
        return slotId;
    }

    private int getVoiceNetworkType() {
        if (this.mServiceState == null) {
            return 0;
        }
        return this.mServiceState.getVoiceNetworkType();
    }

    private int getDataNetworkType() {
        if (this.mServiceState == null) {
            return 0;
        }
        return this.mServiceState.getDataNetworkType();
    }

    public void setImsRegister(CallbackHandler callbackHandler, boolean imsReg) {
        ((MobileState) this.mCurrentState).imsRegister = imsReg;
        callbackHandler.setIsImsRegisted(this.mSlotId, imsReg);
    }

    public void setSpeechHd(CallbackHandler callbackHandler, boolean hd) {
        ((MobileState) this.mCurrentState).speedHd = hd;
        callbackHandler.setSpeechHd(this.mSlotId, hd);
    }

    public void setVowifi(CallbackHandler callbackHandler, boolean vowifi) {
        ((MobileState) this.mCurrentState).vowifi = vowifi;
        callbackHandler.setVowifi(this.mSlotId, vowifi);
    }

    private int getVoiceSignalLevel() {
        if (this.mSignalStrength == null) {
            return 0;
        }
        return isCdma() ? this.mSignalStrength.getCdmaLevel() : this.mSignalStrength.getGsmLevel();
    }

    private boolean showDataAndVoice() {
        if (this.mStyle != 1) {
            return false;
        }
        int dataType = getDataNetworkType();
        int voiceType = getVoiceNetworkType();
        return (dataType == 5 || dataType == 5 || dataType == 6 || dataType == 12 || dataType == 14 || dataType == 13 || dataType == 19) && (voiceType == 16 || voiceType == 7 || voiceType == 4);
    }

    private int getAlternateLteLevel(SignalStrength signalStrength) {
        int lteRsrp = signalStrength.getLteDbm();
        int rsrpLevel = 0;
        if (lteRsrp > -44) {
            rsrpLevel = 0;
        } else if (lteRsrp >= -97) {
            rsrpLevel = 4;
        } else if (lteRsrp >= -105) {
            rsrpLevel = 3;
        } else if (lteRsrp >= -113) {
            rsrpLevel = 2;
        } else if (lteRsrp >= -120) {
            rsrpLevel = 1;
        } else if (lteRsrp >= -140) {
            rsrpLevel = 0;
        }
        if (DEBUG) {
            String str = this.mTag;
            Log.d(str, "getAlternateLteLevel lteRsrp:" + lteRsrp + " rsrpLevel = " + rsrpLevel);
        }
        return rsrpLevel;
    }

    private String getResourceName(int resId) {
        if (resId == 0) {
            return "(null)";
        }
        try {
            return this.mContext.getResources().getResourceName(resId);
        } catch (Resources.NotFoundException e) {
            return "(unknown)";
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setActivity(int activity) {
        boolean z = false;
        ((MobileState) this.mCurrentState).activityIn = activity == 3 || activity == 1;
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (activity == 3 || activity == 2) {
            z = true;
        }
        mobileState.activityOut = z;
        if (this.mConfig.readIconsFromXml) {
            ((MobileState) this.mCurrentState).dataActivity = activity;
        }
        notifyListenersIfNecessary();
    }

    public void dump(PrintWriter pw) {
        super.dump(pw);
        pw.println("  mSubscription=" + this.mSubscriptionInfo + ",");
        pw.println("  mServiceState=" + this.mServiceState + ",");
        pw.println("  mSignalStrength=" + this.mSignalStrength + ",");
    }

    private void updateVoiceType(int slot, int type) {
        String networkNameVoice = "";
        if (hasService()) {
            int networkClass = TelephonyManagerEx.getDefault().getNetworkClass(type);
            if (networkClass == 3) {
                if (!Build.IS_CT_CUSTOMIZATION_TEST || !SUPPORT_CA || !isCdma()) {
                    networkNameVoice = TelephonyIcons.getNetworkTypeName(6, this.mSlotId);
                } else {
                    networkNameVoice = TelephonyIcons.getNetworkTypeName(7, this.mSlotId);
                }
            } else if (networkClass == 2) {
                networkNameVoice = TelephonyIcons.getNetworkTypeName(3, this.mSlotId);
            } else if (Build.IS_CM_CUSTOMIZATION_TEST || Build.IS_CU_CUSTOMIZATION_TEST || Build.IS_CU_CUSTOMIZATION_TEST) {
                networkNameVoice = TelephonyIcons.getNetworkTypeName(1, this.mSlotId);
            } else {
                networkNameVoice = "";
            }
        }
        ((MobileState) this.mCurrentState).networkNameVoice = networkNameVoice;
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateDataType(int r21, int r22, boolean r23, boolean r24, boolean r25, int r26) {
        /*
            r20 = this;
            r0 = r20
            r1 = r22
            java.lang.String[] r2 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeArray
            r2 = r2[r21]
            android.content.res.Resources r3 = r0.mRes
            java.lang.String r4 = "com.android.systemui"
            r5 = 0
            int r3 = r3.getIdentifier(r2, r5, r4)
            android.content.res.Resources r4 = r0.mRes
            java.lang.String[] r4 = r4.getStringArray(r3)
            android.telephony.ServiceState r6 = r0.mServiceState
            if (r6 == 0) goto L_0x0022
            android.telephony.ServiceState r6 = r0.mServiceState
            java.lang.String r6 = r6.getOperatorNumeric()
            goto L_0x0023
        L_0x0022:
            r6 = r5
        L_0x0023:
            r7 = 2131232411(0x7f08069b, float:1.808093E38)
            r8 = 7
            r9 = 9
            r10 = 6
            r13 = 1
            r14 = 2
            r15 = 0
            switch(r1) {
                case 0: goto L_0x02f1;
                case 1: goto L_0x029f;
                case 2: goto L_0x0316;
                case 3: goto L_0x033e;
                case 4: goto L_0x0221;
                case 5: goto L_0x0277;
                case 6: goto L_0x0277;
                case 7: goto L_0x024c;
                case 8: goto L_0x00b1;
                case 9: goto L_0x00b1;
                case 10: goto L_0x00b1;
                case 11: goto L_0x0030;
                case 12: goto L_0x0277;
                case 13: goto L_0x0048;
                case 14: goto L_0x0277;
                case 15: goto L_0x00b1;
                case 16: goto L_0x029f;
                case 17: goto L_0x033e;
                case 18: goto L_0x0030;
                case 19: goto L_0x0048;
                default: goto L_0x0030;
            }
        L_0x0030:
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r5[r21] = r15
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            r5[r21] = r15
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r15
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String r7 = ""
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r15
            goto L_0x0367
        L_0x0048:
            if (r24 == 0) goto L_0x0089
            int[] r9 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r9[r21] = r10
            int[] r9 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r10 = r0.mRes
            java.lang.String[] r12 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationArray
            r12 = r12[r13]
            java.lang.String r11 = "com.android.systemui"
            int r10 = r10.getIdentifier(r12, r5, r11)
            r9[r21] = r10
            r9 = 19
            if (r1 != r9) goto L_0x0076
            int[] r9 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r9[r21] = r8
            int[] r8 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r9 = r0.mRes
            java.lang.String[] r10 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationArray
            r10 = r10[r14]
            java.lang.String r11 = "com.android.systemui"
            int r5 = r9.getIdentifier(r10, r5, r11)
            r8[r21] = r5
        L_0x0076:
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationDescArray
            r7 = r7[r13]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r7 = 3
            r5[r21] = r7
            goto L_0x0367
        L_0x0089:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r7[r21] = r9
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232417(0x7f0806a1, float:1.8080943E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r7 = 3
            r5[r21] = r7
            goto L_0x0367
        L_0x00b1:
            java.util.List<java.lang.String> r8 = r0.mMccNncList
            boolean r8 = r8.contains(r6)
            r11 = 10
            r16 = 4
            r17 = 2131232415(0x7f08069f, float:1.8080939E38)
            r18 = 5
            if (r8 == 0) goto L_0x0178
            android.telephony.ServiceState r8 = r0.mServiceState
            if (r8 == 0) goto L_0x00f4
            android.telephony.ServiceState r8 = r0.mServiceState
            int r8 = r8.getRilDataRadioTechnology()
            r12 = 20
            if (r8 != r12) goto L_0x00f4
            int[] r8 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8[r21] = r10
            int[] r8 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r9 = r0.mRes
            r10 = r4[r1]
            java.lang.String r11 = "com.android.systemui"
            int r5 = r9.getIdentifier(r10, r5, r11)
            r8[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r7 = 3
            r5[r21] = r7
            goto L_0x0146
        L_0x00f4:
            if (r1 == r11) goto L_0x0124
            if (r1 != r9) goto L_0x00f9
            goto L_0x0124
        L_0x00f9:
            r7 = 8
            if (r1 == r7) goto L_0x0101
            r7 = 15
            if (r1 != r7) goto L_0x0146
        L_0x0101:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r7[r21] = r18
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r17
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r18
            goto L_0x0146
        L_0x0124:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r7[r21] = r16
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r17
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r16
        L_0x0146:
            java.lang.String r5 = "MobileSignalController"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "datatype = "
            r7.append(r8)
            java.lang.String r8 = android.telephony.TelephonyManager.getNetworkTypeName(r22)
            r7.append(r8)
            java.lang.String r8 = "; show datatype="
            r7.append(r8)
            int r8 = r0.mSlotId
            java.lang.String r8 = com.android.systemui.statusbar.policy.TelephonyIcons.getNetworkTypeName(r1, r8)
            r7.append(r8)
            java.lang.String r8 = "; networkOperator="
            r7.append(r8)
            r7.append(r6)
            java.lang.String r7 = r7.toString()
            android.util.Log.d(r5, r7)
            goto L_0x0367
        L_0x0178:
            r7 = 8
            if (r1 == r7) goto L_0x01d1
            if (r1 == r9) goto L_0x01d1
            if (r1 != r11) goto L_0x0181
            goto L_0x01d1
        L_0x0181:
            if (r25 == 0) goto L_0x01a7
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r7[r21] = r18
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r17
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r18
            goto L_0x0367
        L_0x01a7:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8 = 3
            r7[r21] = r8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            java.lang.String[] r9 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationArray
            r9 = r9[r15]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232410(0x7f08069a, float:1.8080928E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationDescArray
            r7 = r7[r15]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r14
            goto L_0x0367
        L_0x01d1:
            if (r25 == 0) goto L_0x01f7
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r7[r21] = r16
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r17
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r16
            goto L_0x0367
        L_0x01f7:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8 = 3
            r7[r21] = r8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            java.lang.String[] r9 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationArray
            r9 = r9[r15]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232410(0x7f08069a, float:1.8080928E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationDescArray
            r7 = r7[r15]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r14
            goto L_0x0367
        L_0x0221:
            if (r23 != 0) goto L_0x024c
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r9 = 8
            r7[r21] = r9
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r9 = r0.mRes
            r10 = r4[r1]
            java.lang.String r11 = "com.android.systemui"
            int r5 = r9.getIdentifier(r10, r5, r11)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232409(0x7f080699, float:1.8080926E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r8
            goto L_0x0367
        L_0x024c:
            if (r23 != 0) goto L_0x0277
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8 = 8
            r7[r21] = r8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r11 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r11)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232409(0x7f080699, float:1.8080926E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r10
            goto L_0x0367
        L_0x0277:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8 = 3
            r7[r21] = r8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232410(0x7f08069a, float:1.8080928E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r14
            goto L_0x0367
        L_0x029f:
            if (r23 != 0) goto L_0x02c8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r7[r21] = r13
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232414(0x7f08069e, float:1.8080937E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r15
            goto L_0x0367
        L_0x02c8:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8 = 3
            r7[r21] = r8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            java.lang.String[] r9 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationArray
            r9 = r9[r15]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232410(0x7f08069a, float:1.8080928E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeGenerationDescArray
            r7 = r7[r15]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r14
            goto L_0x0367
        L_0x02f1:
            if (r23 != 0) goto L_0x0316
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r5[r21] = r15
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r5[r21] = r15
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r15
            goto L_0x0367
        L_0x0316:
            if (r23 != 0) goto L_0x033e
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232413(0x7f08069d, float:1.8080935E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r5[r21] = r14
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r5[r21] = r13
            goto L_0x0367
        L_0x033e:
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r8 = 3
            r7[r21] = r8
            int[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            android.content.res.Resources r8 = r0.mRes
            r9 = r4[r1]
            java.lang.String r10 = "com.android.systemui"
            int r5 = r8.getIdentifier(r9, r5, r10)
            r7[r21] = r5
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedQSDataTypeIcon
            r7 = 2131232410(0x7f08069a, float:1.8080928E38)
            r5[r21] = r7
            java.lang.String[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeDesc
            java.lang.String[] r7 = com.android.systemui.statusbar.policy.TelephonyIcons.mDataTypeDescriptionArray
            r7 = r7[r1]
            r5[r21] = r7
            int[] r5 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedSignalStreagthIndex
            r7 = 8
            r5[r21] = r7
        L_0x0367:
            java.lang.String r5 = "MobileSignalController"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "updateDataType "
            r7.append(r8)
            java.lang.String r8 = "mSelectedDataTypeIcon[%d]=%d, mSelectedDataActivityIndex=%d"
            r9 = 3
            java.lang.Object[] r9 = new java.lang.Object[r9]
            java.lang.Integer r10 = java.lang.Integer.valueOf(r21)
            r9[r15] = r10
            int[] r10 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataTypeIcon
            r10 = r10[r21]
            java.lang.Integer r10 = java.lang.Integer.valueOf(r10)
            r9[r13] = r10
            int[] r10 = com.android.systemui.statusbar.policy.TelephonyIcons.mSelectedDataActivityIndex
            r10 = r10[r21]
            java.lang.Integer r10 = java.lang.Integer.valueOf(r10)
            r9[r14] = r10
            java.lang.String r8 = java.lang.String.format(r8, r9)
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            android.util.Log.d(r5, r7)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.MobileSignalController.updateDataType(int, int, boolean, boolean, boolean, int):void");
    }

    public void setMobileTypeListener(NetworkController.MobileTypeListener mobileTypeListener) {
        this.mMobileTypeListener = mobileTypeListener;
    }
}
