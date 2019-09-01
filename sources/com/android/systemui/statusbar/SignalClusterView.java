package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Constants;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.miui.statusbar.WifiLabelText;
import com.android.systemui.statusbar.phone.SignalClusterViewController;
import com.android.systemui.statusbar.phone.StatusBarFactory;
import com.android.systemui.statusbar.phone.StatusBarIconControllerHelper;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.statusbar.policy.DemoModeController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.TelephonyIcons;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.DisableStateTracker;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miui.telephony.SubscriptionInfo;

public class SignalClusterView extends LinearLayout implements DemoMode, DarkIconDispatcher.DarkReceiver, HotspotController.Callback, NetworkController.SignalCallback, SecurityController.SecurityControllerCallback, TunerService.Tunable {
    static final boolean DEBUG = Log.isLoggable("SignalClusterView", 3);
    private static int sFilterColor = 0;
    private boolean mActivityEnabled;
    ImageView mAirplane;
    private String mAirplaneContentDescription;
    private int mAirplaneIconId;
    /* access modifiers changed from: private */
    public boolean mBigRoamEnable;
    private boolean mBlockAirplane;
    private boolean mBlockEthernet;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    /* access modifiers changed from: private */
    public SignalClusterViewController mController;
    /* access modifiers changed from: private */
    public float mDarkIntensity;
    boolean[] mDataConnectedStatus;
    private final DemoModeController.DemoModeCallback mDemoCallback;
    ImageView mDemoMobileSignal;
    private boolean mDemoMode;
    String[] mDualSignalDescription;
    private final int mEndPadding;
    private final int mEndPaddingNothingVisible;
    ImageView mEthernet;
    private boolean mEthernetAble;
    ImageView mEthernetDark;
    private String mEthernetDescription;
    ViewGroup mEthernetGroup;
    private int mEthernetIconId;
    private boolean mEthernetVisible;
    private boolean mForceBlockWifi;
    /* access modifiers changed from: private */
    public boolean mHideVolte;
    private final HotspotController mHotspot;
    private final float mIconScaleFactor;
    private int mIconTint;
    /* access modifiers changed from: private */
    public boolean mIsAirplaneMode;
    private int mLastAirplaneIconId;
    private int mLastEthernetIconId;
    private int mLastWifiActivityId;
    private int mLastWifiBadgeId;
    private int mLastWifiStrengthId;
    private final int mMobileDataIconStartPadding;
    LinearLayout[] mMobileSignalGroup;
    private final int mMobileSignalGroupEndPadding;
    private int mMobileTypeSpace;
    private final NetworkController mNetworkController;
    ImageView mNoSims;
    View mNoSimsCombo;
    ImageView mNoSimsDark;
    private int mNoSimsIcon;
    private boolean mNoSimsVisible;
    /* access modifiers changed from: private */
    public boolean mNotchEar;
    /* access modifiers changed from: private */
    public boolean mNotchEarDual;
    private boolean mNotchEarDualEnable;
    /* access modifiers changed from: private */
    public ArrayList<PhoneState> mPhoneStates;
    private boolean mReadIconsFromXML;
    private final int mSecondaryTelephonyPadding;
    private final SecurityController mSecurityController;
    ViewGroup mSignalDualNotchGroup;
    ImageView mSignalDualNotchMobile;
    ImageView mSignalDualNotchMobile2;
    ImageView mSignalDualNotchMobileInout;
    TextView mSignalDualNotchMobileType;
    ImageView mSignalDualNotchMobileUpgrade;
    TextView mSignalDualNotchMobileVoice;
    ImageView mSignalSimpleDualMobile1;
    ImageView mSignalSimpleDualMobile2;
    ViewGroup mSignalSimpleDualMobileContainer;
    boolean mSignalSimpleDualShowing;
    private int mSimCnt;
    /* access modifiers changed from: private */
    public boolean mSimpleDualMobileEnable;
    /* access modifiers changed from: private */
    public final Rect mTintArea;
    /* access modifiers changed from: private */
    public boolean mUseVolteType1;
    private boolean mVoWifiEnableInEar;
    ImageView[] mVowifi;
    ImageView mVpn;
    private boolean mVpnEnableInEar;
    /* access modifiers changed from: private */
    public boolean mVpnVisible;
    private final int mWideTypeIconStartPadding;
    ImageView mWifi;
    ImageView mWifiActivity;
    private boolean mWifiActivityEnabled;
    ImageView mWifiAp;
    ImageView mWifiApConnectMark;
    private int mWifiBadgeId;
    private String mWifiDescription;
    ViewGroup mWifiGroup;
    private boolean mWifiIn;
    WifiLabelText mWifiLabel;
    private String mWifiName;
    /* access modifiers changed from: private */
    public boolean mWifiNoNetwork;
    private boolean mWifiNoNetworkEnableInEar;
    private boolean mWifiOut;
    View mWifiSignalSpacer;
    private int mWifiStrengthId;
    /* access modifiers changed from: private */
    public boolean mWifiVisible;

    protected class PhoneState {
        public boolean mActivityIn;
        public boolean mActivityOut;
        /* access modifiers changed from: private */
        public int mDataActivityId = 0;
        private boolean mDataConnected;
        private boolean mIsDefaultDataSim;
        public boolean mIsImsRegistered;
        /* access modifiers changed from: private */
        public boolean mIsMobileTypeIconWide;
        private int mLastMobileStrengthId = -1;
        /* access modifiers changed from: private */
        public int mLastMobileTypeId = -1;
        private ImageView mMobile;
        /* access modifiers changed from: private */
        public String mMobileDescription;
        protected ViewGroup mMobileGroup;
        private ImageView mMobileInOut;
        private int mMobileInOutId = 0;
        private ImageView mMobileRoaming;
        private ImageView mMobileSignalUpgrade;
        public int mMobileStrengthId = 0;
        /* access modifiers changed from: private */
        public TextView mMobileType;
        /* access modifiers changed from: private */
        public String mMobileTypeDescription;
        public int mMobileTypeId = 0;
        private ImageView mMobileTypeImage;
        public boolean mMobileVisible = false;
        private TextView mMobileVoice;
        private String mMobileVoiceLabel;
        private ImageView mNotchVolte;
        public boolean mRoaming;
        public final int mSlot;
        private ImageView mSmallRoam;
        private ImageView mSpeechHd;
        /* access modifiers changed from: private */
        public int mStackedDataId = 0;
        /* access modifiers changed from: private */
        public int mStackedVoiceId = 0;
        private ImageView mVolte;
        private ImageView mVolteNoService;
        private ImageView mWcdmaCardSlot;

        public PhoneState(int slot, Context context) {
            setViews((ViewGroup) LayoutInflater.from(context).inflate(R.layout.mobile_signal_group, null));
            this.mSlot = slot;
        }

        public void setViews(ViewGroup root) {
            this.mMobileGroup = root;
            this.mMobile = (ImageView) root.findViewById(R.id.mobile_signal);
            this.mMobileType = (TextView) root.findViewById(R.id.mobile_type);
            this.mMobileTypeImage = (ImageView) root.findViewById(R.id.mobile_type_image);
            this.mMobileSignalUpgrade = (ImageView) root.findViewById(R.id.mobile_signal_upgrade);
            this.mVolte = (ImageView) root.findViewById(R.id.volte);
            this.mVolteNoService = (ImageView) root.findViewById(R.id.volte_no_service);
            this.mNotchVolte = (ImageView) root.findViewById(R.id.notch_volte);
            this.mSmallRoam = (ImageView) root.findViewById(R.id.small_roam);
            this.mSpeechHd = (ImageView) root.findViewById(R.id.speech_hd);
            this.mMobileVoice = (TextView) root.findViewById(R.id.carrier);
            this.mMobileRoaming = (ImageView) root.findViewById(R.id.mobile_roaming);
            this.mMobileInOut = (ImageView) root.findViewById(R.id.mobile_inout);
            this.mWcdmaCardSlot = (ImageView) root.findViewById(R.id.card_slot);
            if (SignalClusterView.this.mUseVolteType1) {
                this.mVolte.setImageResource(R.drawable.stat_sys_signal_volte_1);
            }
        }

        /* access modifiers changed from: protected */
        public boolean bigRoamEnable() {
            return SignalClusterView.this.mBigRoamEnable;
        }

        public boolean apply(boolean isSecondaryIcon) {
            if (SignalClusterView.this.mPhoneStates.size() == 2 && SignalClusterView.this.mNotchEar && SignalClusterView.this.notchEarDualEnable()) {
                boolean unused = SignalClusterView.this.mNotchEarDual = true;
            }
            int i = 8;
            if (SignalClusterView.this.mNotchEarDual && SignalClusterView.this.notchEarDualEnable()) {
                this.mMobileGroup.setVisibility(8);
                if (SignalClusterView.this.mSignalDualNotchGroup.getVisibility() != 0) {
                    SignalClusterView.this.mSignalDualNotchGroup.setVisibility(0);
                }
            }
            if (!this.mMobileVisible || SignalClusterView.this.mIsAirplaneMode) {
                SignalClusterView.this.mSignalDualNotchGroup.setVisibility(8);
                this.mMobileGroup.setVisibility(8);
            } else {
                String networkType = TelephonyIcons.getNetworkTypeName(this.mMobileTypeId, this.mSlot);
                this.mDataConnected = this.mDataActivityId != 0;
                SignalClusterView.this.mDataConnectedStatus[this.mSlot] = this.mDataConnected;
                boolean isShowMobileSignalgrade = updateMobileType(networkType);
                if (!SignalClusterView.this.mNotchEarDual) {
                    if (this.mLastMobileStrengthId != this.mMobileStrengthId && SignalClusterView.this.isNotSignalSimpleDualShowing()) {
                        SignalClusterView.this.updateIcon(this.mMobile, this.mMobileStrengthId);
                        this.mLastMobileStrengthId = this.mMobileStrengthId;
                    }
                    boolean isMobileTypeHide = (SignalClusterView.this.mWifiVisible && !SignalClusterView.this.mWifiNoNetwork && !SignalClusterView.this.isBuildTest()) || this.mMobileTypeId == 0;
                    if (isMobileTypeHide) {
                        this.mMobileType.setVisibility(8);
                        this.mMobileTypeImage.setVisibility(8);
                    } else {
                        SignalClusterView.this.mController.updateMobileTypeVisible(this.mMobileType, this.mMobileTypeImage, is4GLTE(networkType));
                    }
                    this.mMobileSignalUpgrade.setVisibility((!isShowMobileSignalgrade || isMobileTypeHide) ? 8 : 0);
                    SignalClusterView.this.updateIcon(this.mMobileSignalUpgrade, R.drawable.stat_sys_signal_upgrade);
                    if ((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.isBuildTest()) && isMobileTypeHide && !TextUtils.isEmpty(this.mMobileVoiceLabel)) {
                        this.mMobileVoice.setText(this.mMobileVoiceLabel);
                        SignalClusterView.this.mController.updateMobileTypeVisible(this.mMobileVoice, this.mMobileTypeImage, is4GLTE(this.mMobileVoiceLabel));
                    } else {
                        this.mMobileVoice.setVisibility(8);
                    }
                    this.mMobileRoaming.setVisibility((SignalClusterView.this.mNotchEar || !this.mRoaming || !bigRoamEnable()) ? 8 : 0);
                    if (!bigRoamEnable()) {
                        this.mSmallRoam.setVisibility((!this.mRoaming || !SignalClusterView.this.isNotSignalSimpleDualShowing()) ? 8 : 0);
                    }
                    if (this.mActivityIn && this.mActivityOut) {
                        this.mMobileInOutId = R.drawable.stat_sys_signal_inout;
                    } else if (!this.mActivityIn && this.mActivityOut) {
                        this.mMobileInOutId = R.drawable.stat_sys_signal_out;
                    } else if (!this.mActivityIn || this.mActivityOut) {
                        this.mMobileInOutId = R.drawable.stat_sys_signal_data;
                    } else {
                        this.mMobileInOutId = R.drawable.stat_sys_signal_in;
                    }
                    SignalClusterView.this.updateIcon(this.mMobileInOut, this.mMobileInOutId);
                    ImageView imageView = this.mMobileInOut;
                    if ((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.mWifiNoNetwork) && this.mDataConnected) {
                        i = 0;
                    }
                    imageView.setVisibility(i);
                    if (!SignalClusterView.this.mNotchEarDual) {
                        this.mMobileGroup.setVisibility(0);
                    }
                } else {
                    if (this.mSlot == 0) {
                        SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobile, Icons.getSignalHalfId(Integer.valueOf(this.mMobileStrengthId)));
                    } else if (this.mSlot == 1) {
                        SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobile2, Icons.getSignalHalfId(Integer.valueOf(this.mMobileStrengthId)));
                    }
                    if (!SignalClusterView.this.mDataConnectedStatus[0] && !SignalClusterView.this.mDataConnectedStatus[1]) {
                        SignalClusterView.this.mSignalDualNotchMobileInout.setVisibility(8);
                        if (this.mIsDefaultDataSim) {
                            SignalClusterView.this.mSignalDualNotchMobileType.setVisibility(((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.mWifiNoNetwork || SignalClusterView.this.isBuildTest()) && this.mMobileTypeId != 0) ? 0 : 8);
                            SignalClusterView.this.mSignalDualNotchMobileUpgrade.setVisibility(8);
                            if ((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.isBuildTest()) && SignalClusterView.this.mSignalDualNotchMobileType.getVisibility() == 8 && !TextUtils.isEmpty(this.mMobileVoiceLabel)) {
                                SignalClusterView.this.mSignalDualNotchMobileVoice.setText(this.mMobileVoiceLabel);
                                SignalClusterView.this.mSignalDualNotchMobileVoice.setVisibility(0);
                            } else {
                                SignalClusterView.this.mSignalDualNotchMobileVoice.setVisibility(8);
                            }
                        }
                    } else if (this.mDataConnected) {
                        if (this.mMobileTypeId == 0) {
                            SignalClusterView.this.mSignalDualNotchMobileType.setVisibility(((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.mWifiNoNetwork || SignalClusterView.this.isBuildTest()) && this.mMobileTypeId != 0) ? 0 : 8);
                            SignalClusterView.this.mSignalDualNotchMobileUpgrade.setVisibility(8);
                            if ((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.isBuildTest()) && SignalClusterView.this.mSignalDualNotchMobileType.getVisibility() == 8 && !TextUtils.isEmpty(this.mMobileVoiceLabel)) {
                                SignalClusterView.this.mSignalDualNotchMobileVoice.setText(this.mMobileVoiceLabel);
                                SignalClusterView.this.mSignalDualNotchMobileVoice.setVisibility(0);
                            } else {
                                SignalClusterView.this.mSignalDualNotchMobileVoice.setVisibility(8);
                            }
                        } else {
                            SignalClusterView.this.mSignalDualNotchMobileVoice.setVisibility(8);
                            SignalClusterView.this.mSignalDualNotchMobileType.setVisibility(((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.mWifiNoNetwork || SignalClusterView.this.isBuildTest()) && this.mMobileTypeId != 0) ? 0 : 8);
                            SignalClusterView.this.mSignalDualNotchMobileUpgrade.setVisibility(isShowMobileSignalgrade ? SignalClusterView.this.mSignalDualNotchMobileType.getVisibility() : 8);
                            SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobileUpgrade, R.drawable.stat_sys_signal_upgrade);
                        }
                        if (this.mActivityIn && this.mActivityOut) {
                            this.mMobileInOutId = R.drawable.stat_sys_signal_inout;
                        } else if (!this.mActivityIn && this.mActivityOut) {
                            this.mMobileInOutId = R.drawable.stat_sys_signal_out;
                        } else if (!this.mActivityIn || this.mActivityOut) {
                            this.mMobileInOutId = R.drawable.stat_sys_signal_data;
                        } else {
                            this.mMobileInOutId = R.drawable.stat_sys_signal_in;
                        }
                        SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobileInout, this.mMobileInOutId);
                        ImageView imageView2 = SignalClusterView.this.mSignalDualNotchMobileInout;
                        if ((!SignalClusterView.this.mWifiVisible || SignalClusterView.this.mWifiNoNetwork) && this.mDataConnected) {
                            i = 0;
                        }
                        imageView2.setVisibility(i);
                    }
                }
                SignalClusterView.this.mDualSignalDescription[this.mSlot] = this.mMobileTypeDescription + " " + this.mMobileDescription;
                this.mMobileGroup.setContentDescription(SignalClusterView.this.mDualSignalDescription[this.mSlot]);
                SignalClusterView.this.mSignalDualNotchGroup.setContentDescription(SignalClusterView.this.mDualSignalDescription[0] + " " + SignalClusterView.this.mDualSignalDescription[1]);
            }
            if (SignalClusterView.DEBUG) {
                Object[] objArr = new Object[3];
                objArr[0] = this.mMobileVisible ? "VISIBLE" : "GONE";
                objArr[1] = Integer.valueOf(this.mMobileStrengthId);
                objArr[2] = Integer.valueOf(this.mMobileTypeId);
                Log.d("SignalClusterView", String.format("mobile: %s sig=%d typ=%d", objArr));
            }
            return this.mMobileVisible;
        }

        public void setMobile(boolean visiable) {
            this.mMobile.setVisibility(visiable ? 0 : 8);
        }

        public void setIsImsRegisted(boolean imsRegisted) {
            int i = 8;
            if (Constants.IS_INTERNATIONAL) {
                ImageView imageView = this.mVolte;
                if (!SignalClusterView.this.mHideVolte && imsRegisted && (!SignalClusterView.this.mNotchEar || (SignalClusterView.this.mSimpleDualMobileEnable && !SignalClusterView.this.mWifiVisible))) {
                    i = 0;
                }
                imageView.setVisibility(i);
                return;
            }
            ImageView imageView2 = this.mNotchVolte;
            if (!SignalClusterView.this.mHideVolte && imsRegisted) {
                i = 0;
            }
            imageView2.setVisibility(i);
        }

        public void setVolteNoService(boolean show) {
            if (!SignalClusterView.this.mNotchEar) {
                this.mVolteNoService.setVisibility(show ? 0 : 8);
            }
        }

        public void setSpeechHd(boolean hd) {
            if (!SignalClusterView.this.mNotchEar || SignalClusterView.this.isCUDripBuildTEST()) {
                this.mSpeechHd.setVisibility(hd ? 0 : 8);
            }
        }

        public void setNetworkNameVoice(String networkNameVoice) {
            this.mMobileVoiceLabel = networkNameVoice;
        }

        public void setIsDefaultDataSim(boolean isDefaultDataSim) {
            this.mIsDefaultDataSim = isDefaultDataSim;
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (this.mMobileVisible && this.mMobileGroup != null && this.mMobileGroup.getContentDescription() != null) {
                event.getText().add(this.mMobileGroup.getContentDescription());
            }
        }

        public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
            setTextColor();
            if (!SignalClusterView.this.mNotchEarDual) {
                SignalClusterView.this.updateIcon(this.mMobileRoaming, R.drawable.stat_sys_data_connected_roam);
                SignalClusterView.this.updateIcon(this.mMobileInOut, this.mMobileInOutId);
                SignalClusterView.this.updateIcon(this.mMobile, this.mMobileStrengthId);
                SignalClusterView.this.updateIcon(this.mMobileSignalUpgrade, R.drawable.stat_sys_signal_upgrade);
                SignalClusterView.this.updateIcon(this.mNotchVolte, R.drawable.stat_sys_signal_hd_notch);
                SignalClusterView.this.updateIcon(this.mSmallRoam, R.drawable.stat_sys_data_connected_roam_small);
                SignalClusterView.this.updateIcon(this.mVolte, SignalClusterView.this.mUseVolteType1 ? R.drawable.stat_sys_signal_volte_1 : R.drawable.stat_sys_signal_volte);
                SignalClusterView.this.updateIcon(this.mSpeechHd, R.drawable.stat_sys_speech_hd);
                SignalClusterView.this.updateIcon(this.mVolteNoService, R.drawable.stat_sys_volte_no_service);
                SignalClusterView.this.updateIcon(this.mMobileTypeImage, R.drawable.stat_sys_signal_4g_lte);
                return;
            }
            if (this.mSlot == 0) {
                SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobile, Icons.getSignalHalfId(Integer.valueOf(this.mMobileStrengthId)));
            } else if (this.mSlot == 1) {
                SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobile2, Icons.getSignalHalfId(Integer.valueOf(this.mMobileStrengthId)));
            }
            if (this.mDataConnected) {
                SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobileInout, this.mMobileInOutId);
                SignalClusterView.this.updateIcon(SignalClusterView.this.mSignalDualNotchMobileUpgrade, R.drawable.stat_sys_signal_upgrade);
            }
        }

        public void setTextColor() {
            boolean showCtsSpecifiedColor = Util.showCtsSpecifiedColor();
            boolean z = false;
            int i = R.color.status_bar_textColor;
            if (showCtsSpecifiedColor) {
                if (!SignalClusterView.this.mNotchEarDual) {
                    this.mMobileType.setTextColor(SignalClusterView.this.mContext.getResources().getColor((DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, this.mMobileType, SignalClusterView.this.mDarkIntensity) > 0.0f ? 1 : (DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, this.mMobileType, SignalClusterView.this.mDarkIntensity) == 0.0f ? 0 : -1)) > 0 ? R.color.status_bar_icon_text_color_dark_mode_cts : R.color.status_bar_textColor));
                    if (DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, this.mMobileVoice, SignalClusterView.this.mDarkIntensity) > 0.0f) {
                        z = true;
                    }
                    boolean isDark = z;
                    TextView textView = this.mMobileVoice;
                    Resources resources = SignalClusterView.this.mContext.getResources();
                    if (isDark) {
                        i = R.color.status_bar_icon_text_color_dark_mode_cts;
                    }
                    textView.setTextColor(resources.getColor(i));
                    return;
                }
                if (DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, SignalClusterView.this.mSignalDualNotchMobileType, SignalClusterView.this.mDarkIntensity) > 0.0f) {
                    z = true;
                }
                boolean isDark2 = z;
                TextView textView2 = SignalClusterView.this.mSignalDualNotchMobileType;
                Resources resources2 = SignalClusterView.this.mContext.getResources();
                if (isDark2) {
                    i = R.color.status_bar_icon_text_color_dark_mode_cts;
                }
                textView2.setTextColor(resources2.getColor(i));
            } else if (!SignalClusterView.this.mNotchEarDual) {
                this.mMobileType.setTextColor(SignalClusterView.this.mContext.getResources().getColor((DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, this.mMobileType, SignalClusterView.this.mDarkIntensity) > 0.0f ? 1 : (DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, this.mMobileType, SignalClusterView.this.mDarkIntensity) == 0.0f ? 0 : -1)) > 0 ? R.color.status_bar_textColor_darkmode : R.color.status_bar_textColor));
                if (DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, this.mMobileVoice, SignalClusterView.this.mDarkIntensity) > 0.0f) {
                    z = true;
                }
                boolean isDark3 = z;
                TextView textView3 = this.mMobileVoice;
                Resources resources3 = SignalClusterView.this.mContext.getResources();
                if (isDark3) {
                    i = R.color.status_bar_textColor_darkmode;
                }
                textView3.setTextColor(resources3.getColor(i));
            } else {
                if (DarkIconDispatcherHelper.getDarkIntensity(SignalClusterView.this.mTintArea, SignalClusterView.this.mSignalDualNotchMobileType, SignalClusterView.this.mDarkIntensity) > 0.0f) {
                    z = true;
                }
                boolean isDark4 = z;
                SignalClusterView.this.mSignalDualNotchMobileType.setTextColor(SignalClusterView.this.mContext.getResources().getColor(isDark4 ? R.color.status_bar_textColor_darkmode : R.color.status_bar_textColor));
                TextView textView4 = SignalClusterView.this.mSignalDualNotchMobileVoice;
                Resources resources4 = SignalClusterView.this.mContext.getResources();
                if (isDark4) {
                    i = R.color.status_bar_textColor_darkmode;
                }
                textView4.setTextColor(resources4.getColor(i));
            }
        }

        private boolean updateMobileType(String networkType) {
            boolean is4Gplus = is4Gplus(networkType);
            if (is4Gplus) {
                networkType = "4G";
            }
            if (!SignalClusterView.this.mNotchEarDual) {
                return updateMobileTypeForNormal(is4Gplus, networkType);
            }
            return updateMobileTypeForNotchEarDual(is4Gplus, networkType);
        }

        private boolean is4Gplus(String networkType) {
            return networkType.equals("4G+");
        }

        private boolean updateMobileTypeForNotchEarDual(boolean is4Gplus, String networkType) {
            boolean isShow = this.mDataConnected || (!SignalClusterView.this.mDataConnectedStatus[0] && !SignalClusterView.this.mDataConnectedStatus[1] && this.mIsDefaultDataSim);
            boolean isTypeChanged = !networkType.equals(SignalClusterView.this.mSignalDualNotchMobileType.getText());
            if (isShow && isTypeChanged) {
                this.mLastMobileTypeId = this.mMobileTypeId;
                SignalClusterView.this.mSignalDualNotchMobileType.setText(networkType);
            }
            if (!isShow || !is4Gplus) {
                return false;
            }
            return true;
        }

        private boolean updateMobileTypeForNormal(boolean is4Gplus, String networkType) {
            if (this.mLastMobileTypeId != this.mMobileTypeId || (is4Gplus && !networkType.equals(this.mMobileType.getText()))) {
                this.mLastMobileTypeId = this.mMobileTypeId;
                this.mMobileType.setText(networkType);
            }
            return is4Gplus;
        }

        private boolean is4GLTE(String networkType) {
            return "4G LTE".equals(networkType);
        }
    }

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mNoSimsVisible = false;
        this.mVpnVisible = false;
        this.mEthernetVisible = false;
        this.mEthernetIconId = 0;
        this.mLastEthernetIconId = -1;
        this.mWifiBadgeId = -1;
        this.mWifiVisible = false;
        this.mWifiNoNetwork = false;
        this.mWifiStrengthId = 0;
        this.mLastWifiBadgeId = -1;
        this.mLastWifiStrengthId = -1;
        this.mLastWifiActivityId = -1;
        this.mIsAirplaneMode = false;
        this.mAirplaneIconId = 0;
        this.mLastAirplaneIconId = -1;
        this.mPhoneStates = new ArrayList<>();
        this.mIconTint = -1;
        this.mTintArea = new Rect();
        this.mVowifi = new ImageView[2];
        this.mDualSignalDescription = new String[2];
        this.mDataConnectedStatus = new boolean[2];
        this.mMobileSignalGroup = new LinearLayout[2];
        this.mSignalSimpleDualShowing = false;
        this.mDemoCallback = new DemoModeController.DemoModeCallback() {
            public void onDemoModeChanged(String command, Bundle args) {
                SignalClusterView.this.dispatchDemoCommand(command, args);
            }
        };
        Resources res = getResources();
        this.mHideVolte = res.getBoolean(R.bool.status_bar_hide_volte);
        this.mUseVolteType1 = res.getBoolean(R.bool.use_volte_image_type_1);
        this.mMobileSignalGroupEndPadding = res.getDimensionPixelSize(R.dimen.mobile_signal_group_end_padding);
        this.mMobileDataIconStartPadding = res.getDimensionPixelSize(R.dimen.mobile_data_icon_start_padding);
        this.mWideTypeIconStartPadding = res.getDimensionPixelSize(R.dimen.wide_type_icon_start_padding);
        this.mSecondaryTelephonyPadding = res.getDimensionPixelSize(R.dimen.secondary_telephony_padding);
        this.mEndPadding = res.getDimensionPixelSize(R.dimen.signal_cluster_battery_padding);
        this.mEndPaddingNothingVisible = res.getDimensionPixelSize(R.dimen.no_signal_cluster_battery_padding);
        this.mNotchEarDualEnable = res.getBoolean(R.bool.config_notch_ear_dual_enabled);
        this.mEthernetAble = res.getBoolean(R.bool.config_ethernet_enabled);
        this.mBigRoamEnable = res.getBoolean(R.bool.config_big_roam_enabled);
        this.mSimpleDualMobileEnable = res.getBoolean(R.bool.config_simple_dual_enabled);
        this.mWifiNoNetworkEnableInEar = res.getBoolean(R.bool.config_wifi_no_network_in_ear_enabled);
        this.mVpnEnableInEar = res.getBoolean(R.bool.config_vpn_in_ear_enabled);
        this.mVoWifiEnableInEar = res.getBoolean(R.bool.config_vowifi_in_ear_enabled);
        TypedValue typedValue = new TypedValue();
        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        this.mIconScaleFactor = typedValue.getFloat();
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        addOnAttachStateChangeListener(new DisableStateTracker(0, 2));
        updateActivityEnabled();
        this.mReadIconsFromXML = res.getBoolean(R.bool.config_read_icons_from_xml);
        this.mHotspot = (HotspotController) Dependency.get(HotspotController.class);
        this.mController = StatusBarFactory.getInstance().getSignalClusterViewController();
    }

    public void onTuningChanged(String key, String newValue) {
        if ("icon_blacklist".equals(key)) {
            ArraySet<String> blockList = StatusBarIconControllerHelper.getIconBlacklist(newValue);
            boolean blockAirplane = blockList.contains("airplane");
            boolean blockMobile = blockList.contains("mobile");
            boolean blockWifi = blockList.contains("wifi");
            boolean blockEthernet = blockList.contains("ethernet");
            if (!(blockAirplane == this.mBlockAirplane && blockMobile == this.mBlockMobile && blockEthernet == this.mBlockEthernet && blockWifi == this.mBlockWifi)) {
                this.mBlockAirplane = blockAirplane;
                this.mBlockMobile = blockMobile;
                this.mBlockEthernet = blockEthernet;
                this.mBlockWifi = blockWifi || this.mForceBlockWifi;
                this.mNetworkController.removeCallback(this);
                this.mNetworkController.addCallback(this);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mVpn = (ImageView) findViewById(R.id.vpn);
        this.mEthernetGroup = (ViewGroup) findViewById(R.id.ethernet_combo);
        this.mEthernet = (ImageView) findViewById(R.id.ethernet);
        this.mEthernetDark = (ImageView) findViewById(R.id.ethernet_dark);
        this.mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        this.mWifi = (ImageView) findViewById(R.id.wifi_signal);
        this.mWifiActivity = (ImageView) findViewById(R.id.wifi_inout);
        this.mWifiAp = (ImageView) findViewById(R.id.wifi_ap_on);
        this.mWifiLabel = (WifiLabelText) findViewById(R.id.wifi_label);
        this.mAirplane = (ImageView) findViewById(R.id.airplane);
        this.mVowifi[0] = (ImageView) findViewById(R.id.vowifi_0);
        this.mVowifi[1] = (ImageView) findViewById(R.id.vowifi_1);
        this.mNoSims = (ImageView) findViewById(R.id.no_sims);
        this.mDemoMobileSignal = (ImageView) findViewById(R.id.demo_mobile_signal);
        this.mNoSimsDark = (ImageView) findViewById(R.id.no_sims_dark);
        this.mNoSimsCombo = findViewById(R.id.no_sims_combo);
        this.mWifiSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        this.mMobileSignalGroup[0] = (LinearLayout) findViewById(R.id.mobile_signal_group_0);
        this.mMobileSignalGroup[1] = (LinearLayout) findViewById(R.id.mobile_signal_group_1);
        this.mWifiApConnectMark = (ImageView) findViewById(R.id.wifi_ap_connect_mark);
        this.mSignalDualNotchGroup = (ViewGroup) findViewById(R.id.mobile_signal_group_dual_notch);
        this.mSignalDualNotchMobile = (ImageView) this.mSignalDualNotchGroup.findViewById(R.id.notch_mobile_signal);
        this.mSignalDualNotchMobile2 = (ImageView) this.mSignalDualNotchGroup.findViewById(R.id.notch_mobile_signal2);
        this.mSignalDualNotchMobileVoice = (TextView) this.mSignalDualNotchGroup.findViewById(R.id.carrier);
        this.mSignalDualNotchMobileType = (TextView) this.mSignalDualNotchGroup.findViewById(R.id.mobile_type);
        this.mSignalDualNotchMobileInout = (ImageView) this.mSignalDualNotchGroup.findViewById(R.id.mobile_inout);
        this.mSignalDualNotchMobileUpgrade = (ImageView) this.mSignalDualNotchGroup.findViewById(R.id.mobile_signal_upgrade);
        this.mSignalSimpleDualMobileContainer = (ViewGroup) findViewById(R.id.mobile_simple_dual_mobile_container);
        this.mSignalSimpleDualMobile1 = (ImageView) this.mSignalSimpleDualMobileContainer.findViewById(R.id.simple_dual_mobile_signal1);
        this.mSignalSimpleDualMobile2 = (ImageView) this.mSignalSimpleDualMobileContainer.findViewById(R.id.simple_dual_mobile_signal2);
        maybeScaleVpnAndNoSimsIcons();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void maybeScaleVpnAndNoSimsIcons() {
        if (this.mIconScaleFactor != 1.0f) {
            this.mVpn.setImageDrawable(new ScalingDrawableWrapper(this.mVpn.getDrawable(), this.mIconScaleFactor));
            this.mNoSims.setImageDrawable(new ScalingDrawableWrapper(this.mNoSims.getDrawable(), this.mIconScaleFactor));
            this.mNoSimsDark.setImageDrawable(new ScalingDrawableWrapper(this.mNoSimsDark.getDrawable(), this.mIconScaleFactor));
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mVpnVisible = isVpnVisible();
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState state = it.next();
            if (state.mSlot < this.mMobileSignalGroup.length && state.mMobileGroup.getParent() == null) {
                this.mMobileSignalGroup[state.mSlot].addView(state.mMobileGroup);
            }
        }
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
        apply();
        applyIconTint();
        this.mNetworkController.addCallback(this);
        this.mSecurityController.addCallback(this);
        this.mHotspot.addCallback(this);
        ((DemoModeController) Dependency.get(DemoModeController.class)).addCallback(this.mDemoCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        for (LinearLayout removeAllViews : this.mMobileSignalGroup) {
            removeAllViews.removeAllViews();
        }
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        this.mSecurityController.removeCallback(this);
        this.mNetworkController.removeCallback(this);
        this.mHotspot.removeCallback(this);
        ((DemoModeController) Dependency.get(DemoModeController.class)).removeCallback(this.mDemoCallback);
        super.onDetachedFromWindow();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        applyIconTint();
    }

    public void onStateChanged() {
        post(new Runnable() {
            public void run() {
                boolean unused = SignalClusterView.this.mVpnVisible = SignalClusterView.this.isVpnVisible();
                SignalClusterView.this.apply();
            }
        });
    }

    /* access modifiers changed from: private */
    public boolean isVpnVisible() {
        return (!this.mNotchEar || this.mVpnEnableInEar) && this.mSecurityController.isVpnEnabled() && !this.mSecurityController.isSilentVpnPackage();
    }

    public void onHotspotChanged(boolean enabled) {
        this.mWifiAp.setVisibility((!enabled || this.mNotchEar) ? 8 : 0);
    }

    private void updateActivityEnabled() {
        this.mActivityEnabled = this.mContext.getResources().getBoolean(R.bool.config_showActivity);
        this.mWifiActivityEnabled = this.mContext.getResources().getBoolean(R.bool.config_showWifiActivity);
    }

    public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description, boolean isTransient) {
        boolean z = false;
        this.mWifiVisible = statusIcon.visible && !this.mBlockWifi;
        this.mWifiStrengthId = statusIcon.icon;
        this.mWifiBadgeId = statusIcon.iconOverlay;
        this.mWifiDescription = statusIcon.contentDescription;
        this.mWifiName = getWifiName(description);
        this.mWifiIn = activityIn && this.mActivityEnabled && this.mWifiVisible;
        this.mWifiOut = activityOut && this.mActivityEnabled && this.mWifiVisible;
        if (this.mWifiStrengthId == R.drawable.stat_sys_wifi_signal_null) {
            z = true;
        }
        this.mWifiNoNetwork = z;
        apply();
    }

    public void setNotchEar() {
        this.mNotchEar = true;
        if (this.mWifiAp != null) {
            this.mWifiAp.setVisibility(8);
        }
        this.mVpnVisible = isVpnVisible();
        if (!this.mVpnEnableInEar && this.mVpn != null) {
            this.mVpn.setVisibility(8);
        }
        if (!this.mVoWifiEnableInEar && this.mVowifi != null) {
            for (int i = 0; i < this.mVowifi.length; i++) {
                if (this.mVowifi[i] != null) {
                    this.mVowifi[i].setVisibility(8);
                }
            }
        }
        if (this.mPhoneStates != null) {
            for (int i2 = 0; i2 < this.mPhoneStates.size(); i2++) {
                if (this.mPhoneStates.get(i2) != null) {
                    int slotId = this.mPhoneStates.get(i2).mSlot;
                    if (couldHideVolte()) {
                        setIsImsRegisted(slotId, false);
                    }
                    setVolteNoService(slotId, false);
                    setSpeechHd(slotId, false);
                }
            }
        }
    }

    public void setMobile(boolean visiable) {
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState state = it.next();
            if (state != null) {
                state.setMobile(visiable);
            }
        }
    }

    public void setIsImsRegisted(int slot, boolean imsRegisted) {
        PhoneState state = getState(slot);
        if (state != null) {
            state.mIsImsRegistered = imsRegisted;
            if (Constants.IS_INTERNATIONAL) {
                computeCapacity();
            }
            state.setIsImsRegisted(imsRegisted);
        }
    }

    public void setVolteNoService(int slot, boolean show) {
        PhoneState state = getState(slot);
        if (state != null) {
            state.setVolteNoService(show);
        }
    }

    public void setSpeechHd(int slot, boolean hd) {
        PhoneState state = getState(slot);
        if (state != null) {
            state.setSpeechHd(hd);
        }
    }

    public void setVowifi(int slot, boolean vowifi) {
        if (slot < this.mVowifi.length) {
            this.mVowifi[slot].setVisibility(((!this.mNotchEar || this.mVoWifiEnableInEar) && vowifi) ? 0 : 8);
        }
    }

    public void setNetworkNameVoice(int slot, String networkNameVoice) {
        PhoneState state = getState(slot);
        if (state != null) {
            state.setNetworkNameVoice(networkNameVoice);
        }
    }

    public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
        PhoneState state = getState(slot);
        if (state != null) {
            state.setIsDefaultDataSim(isDefaultDataSim);
        }
    }

    public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int qsType, boolean activityIn, boolean activityOut, int dataActivityId, int stackedDataId, int stackedVoiceId, String typeContentDescription, String description, boolean isWide, int slot, boolean roaming) {
        NetworkController.IconState iconState = statusIcon;
        int i = statusType;
        PhoneState state = getState(slot);
        if (state != null) {
            boolean z = false;
            state.mMobileVisible = iconState.visible && !this.mBlockMobile;
            state.mMobileStrengthId = iconState.icon;
            state.mMobileTypeId = i;
            String unused = state.mMobileDescription = iconState.contentDescription;
            String unused2 = state.mMobileTypeDescription = typeContentDescription;
            boolean unused3 = state.mIsMobileTypeIconWide = i != 0 && isWide;
            state.mRoaming = roaming;
            state.mActivityIn = activityIn && this.mActivityEnabled;
            if (activityOut && this.mActivityEnabled) {
                z = true;
            }
            state.mActivityOut = z;
            int unused4 = state.mDataActivityId = dataActivityId;
            int unused5 = state.mStackedDataId = stackedDataId;
            int unused6 = state.mStackedVoiceId = stackedVoiceId;
            apply();
        }
    }

    public void setEthernetIndicators(NetworkController.IconState state) {
        this.mEthernetVisible = state.visible && !this.mBlockEthernet;
        this.mEthernetIconId = state.icon;
        this.mEthernetDescription = state.contentDescription;
        apply();
    }

    public void setNoSims(boolean show) {
        this.mNoSimsVisible = show && !this.mBlockMobile;
        apply();
    }

    public void setSubs(List<SubscriptionInfo> subs) {
        if (!hasCorrectSubs(subs)) {
            this.mPhoneStates.clear();
            int i = 0;
            for (int i2 = 0; i2 < this.mVowifi.length; i2++) {
                setVowifi(i2, false);
            }
            for (int i3 = 0; i3 < this.mMobileSignalGroup.length; i3++) {
                if (this.mMobileSignalGroup[i3] != null) {
                    this.mMobileSignalGroup[i3].removeAllViews();
                }
            }
            int n = subs.size();
            for (int i4 = 0; i4 < n; i4++) {
                inflatePhoneState(subs.get(i4).getSlotId());
                int subId = subs.get(i4).getSubscriptionId();
                NetworkController.SignalState signalState = ((NetworkController) Dependency.get(NetworkController.class)).getSignalState();
                if (signalState.imsMap.get(Integer.valueOf(subId)) != null) {
                    setIsImsRegisted(subs.get(i4).getSlotId(), signalState.imsMap.get(Integer.valueOf(subId)).booleanValue());
                }
                if (signalState.vowifiMap.get(Integer.valueOf(subId)) != null) {
                    setVowifi(subs.get(i4).getSlotId(), signalState.vowifiMap.get(Integer.valueOf(subId)).booleanValue());
                }
                if (signalState.speedHdMap.get(Integer.valueOf(subId)) != null) {
                    setSpeechHd(subs.get(i4).getSlotId(), signalState.speedHdMap.get(Integer.valueOf(subId)).booleanValue());
                }
            }
            this.mSimCnt = n;
            if (this.mNotchEar) {
                this.mNotchEarDual = this.mSimCnt == 2 && notchEarDualEnable();
                if (this.mSignalDualNotchGroup != null) {
                    ViewGroup viewGroup = this.mSignalDualNotchGroup;
                    if (!this.mNotchEarDual) {
                        i = 8;
                    }
                    viewGroup.setVisibility(i);
                }
            }
            if (isAttachedToWindow()) {
                applyIconTint();
            }
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> subs) {
        int N = subs.size();
        if (N != this.mPhoneStates.size()) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            if (this.mPhoneStates.get(i).mSlot != subs.get(i).getSlotId()) {
                return false;
            }
        }
        return true;
    }

    private PhoneState getState(int slot) {
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState state = it.next();
            if (state.mSlot == slot) {
                return state;
            }
        }
        Log.e("SignalClusterView", "Unexpected subscription " + slot);
        return null;
    }

    private int getNoSimIcon() {
        Resources res = getContext().getResources();
        if (!res.getBoolean(R.bool.config_read_icons_from_xml)) {
            return 0;
        }
        try {
            String[] noSimArray = res.getStringArray(R.array.multi_no_sim);
            if (noSimArray == null) {
                return 0;
            }
            String resName = noSimArray[0];
            int resId = res.getIdentifier(resName, null, getContext().getPackageName());
            if (DEBUG) {
                Log.d("SignalClusterView", "getNoSimIcon resId = " + resId + " resName = " + resName);
            }
            return resId;
        } catch (Resources.NotFoundException e) {
            return 0;
        }
    }

    private PhoneState inflatePhoneState(int slot) {
        PhoneState state = new PhoneState(slot, this.mContext);
        if (slot <= this.mMobileSignalGroup.length && this.mMobileSignalGroup[slot] != null) {
            this.mMobileSignalGroup[slot].addView(state.mMobileGroup);
        }
        this.mPhoneStates.add(state);
        return state;
    }

    public void setIsAirplaneMode(NetworkController.IconState icon) {
        this.mIsAirplaneMode = icon.visible && !this.mBlockAirplane;
        this.mAirplaneIconId = icon.icon;
        this.mAirplaneContentDescription = icon.contentDescription;
        apply();
    }

    public void setMobileDataEnabled(boolean enabled) {
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (!(!this.mEthernetVisible || this.mEthernetGroup == null || this.mEthernetGroup.getContentDescription() == null)) {
            event.getText().add(this.mEthernetGroup.getContentDescription());
        }
        if (!(!this.mWifiVisible || this.mWifiGroup == null || this.mWifiGroup.getContentDescription() == null)) {
            event.getText().add(this.mWifiGroup.getContentDescription());
        }
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            it.next().populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (this.mEthernet != null) {
            this.mEthernet.setImageDrawable(null);
            this.mEthernetDark.setImageDrawable(null);
            this.mLastEthernetIconId = -1;
        }
        if (this.mWifi != null) {
            this.mWifi.setImageDrawable(null);
            this.mLastWifiStrengthId = -1;
            this.mLastWifiBadgeId = -1;
        }
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState state = it.next();
            if (state.mMobileType != null) {
                int unused = state.mLastMobileTypeId = -1;
            }
        }
        if (this.mAirplane != null) {
            this.mAirplane.setImageDrawable(null);
            this.mLastAirplaneIconId = -1;
        }
        apply();
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: private */
    public void apply() {
        if (this.mWifiGroup != null) {
            int i = 8;
            if (this.mDemoMode) {
                for (int i2 = 0; i2 < getChildCount(); i2++) {
                    getChildAt(i2).setVisibility(8);
                }
                this.mWifiGroup.setVisibility(0);
                this.mDemoMobileSignal.setVisibility(0);
                this.mWifiActivity.setVisibility(8);
                this.mWifiAp.setVisibility(8);
                this.mVowifi[0].setVisibility(8);
                this.mVowifi[1].setVisibility(8);
                this.mWifi.setImageResource(R.drawable.stat_sys_wifi_signal_4);
                this.mDemoMobileSignal.setImageResource(R.drawable.stat_sys_signal_5);
                return;
            }
            for (LinearLayout visibility : this.mMobileSignalGroup) {
                visibility.setVisibility(0);
            }
            this.mDemoMobileSignal.setVisibility(8);
            this.mVpn.setVisibility(this.mVpnVisible ? 0 : 8);
            if (DEBUG) {
                Object[] objArr = new Object[1];
                objArr[0] = this.mVpnVisible ? "VISIBLE" : "GONE";
                Log.d("SignalClusterView", String.format("vpn: %s", objArr));
            }
            if (!this.mEthernetVisible || !ethernetEnable()) {
                this.mEthernetGroup.setVisibility(8);
            } else {
                if (this.mLastEthernetIconId != this.mEthernetIconId) {
                    setIconForView(this.mEthernet, this.mEthernetIconId);
                    setIconForView(this.mEthernetDark, this.mEthernetIconId);
                    this.mLastEthernetIconId = this.mEthernetIconId;
                }
                this.mEthernetGroup.setContentDescription(this.mEthernetDescription);
                this.mEthernetGroup.setVisibility(0);
            }
            if (DEBUG) {
                Object[] objArr2 = new Object[1];
                objArr2[0] = this.mEthernetVisible ? "VISIBLE" : "GONE";
                Log.d("SignalClusterView", String.format("ethernet: %s", objArr2));
            }
            if (this.mWifiVisible && !this.mWifiNoNetwork) {
                if (!(this.mWifiStrengthId == this.mLastWifiStrengthId && this.mWifiBadgeId == this.mLastWifiBadgeId)) {
                    if (this.mWifiBadgeId == -1) {
                        setIconForView(this.mWifi, this.mWifiStrengthId);
                    } else {
                        setBadgedWifiIconForView(this.mWifi, this.mWifiStrengthId, this.mWifiBadgeId);
                    }
                    this.mLastWifiStrengthId = this.mWifiStrengthId;
                    this.mLastWifiBadgeId = this.mWifiBadgeId;
                }
                updateIcon(this.mWifi, this.mWifiStrengthId);
                this.mWifiActivity.setVisibility((this.mWifiIn || this.mWifiOut) ? 0 : 8);
                if (this.mWifiIn && this.mWifiOut) {
                    this.mLastWifiActivityId = R.drawable.stat_sys_wifi_inout;
                } else if (this.mWifiIn) {
                    this.mLastWifiActivityId = R.drawable.stat_sys_wifi_in;
                } else if (this.mWifiOut) {
                    this.mLastWifiActivityId = R.drawable.stat_sys_wifi_out;
                }
                if (this.mLastWifiActivityId != -1) {
                    this.mWifiActivity.setImageResource(Icons.get(Integer.valueOf(this.mLastWifiActivityId), DarkIconDispatcherHelper.inDarkMode(this.mTintArea, this.mWifiActivity, this.mDarkIntensity)));
                }
                this.mWifiLabel.setWifiLabel(false, this.mWifiName);
                this.mWifiGroup.setContentDescription(this.mWifiDescription);
                this.mWifiGroup.setVisibility(0);
            } else if (!this.mWifiVisible || !this.mWifiNoNetwork || (this.mNotchEar && !this.mWifiNoNetworkEnableInEar)) {
                this.mWifiLabel.setWifiLabel(true, this.mWifiName);
                this.mWifiGroup.setVisibility(8);
            } else {
                updateIcon(this.mWifi, this.mWifiStrengthId);
                this.mWifiActivity.setVisibility(8);
                this.mWifiLabel.setWifiLabel(false, this.mWifiName);
                this.mWifiGroup.setContentDescription(this.mWifiDescription);
                this.mWifiGroup.setVisibility(0);
            }
            if (DEBUG) {
                Object[] objArr3 = new Object[2];
                objArr3[0] = this.mWifiVisible ? "VISIBLE" : "GONE";
                objArr3[1] = Integer.valueOf(this.mWifiStrengthId);
                Log.d("SignalClusterView", String.format("wifi: %s sig=%d", objArr3));
            }
            computeCapacity();
            boolean anyMobileVisible = false;
            int firstMobileTypeId = 0;
            Iterator<PhoneState> it = this.mPhoneStates.iterator();
            while (it.hasNext()) {
                PhoneState state = it.next();
                if (state.apply(anyMobileVisible) && !anyMobileVisible) {
                    firstMobileTypeId = state.mMobileTypeId;
                    anyMobileVisible = true;
                }
            }
            if (this.mIsAirplaneMode) {
                if (this.mLastAirplaneIconId != this.mAirplaneIconId) {
                    setIconForView(this.mAirplane, this.mAirplaneIconId);
                    this.mLastAirplaneIconId = this.mAirplaneIconId;
                }
                this.mAirplane.setContentDescription(this.mAirplaneContentDescription);
                this.mAirplane.setVisibility(0);
            } else {
                this.mAirplane.setVisibility(8);
            }
            if (((!anyMobileVisible || firstMobileTypeId == 0) && !this.mNoSimsVisible) || !this.mWifiVisible) {
                this.mWifiSignalSpacer.setVisibility(8);
            } else {
                this.mWifiSignalSpacer.setVisibility(0);
            }
            if (!(!this.mNoSimsVisible || this.mNoSims == null || this.mNoSimsDark == null)) {
                if (this.mNoSimsIcon == 0) {
                    this.mNoSimsIcon = getNoSimIcon();
                }
                if (this.mNoSimsIcon != 0) {
                    this.mNoSims.setImageResource(this.mNoSimsIcon);
                    this.mNoSimsDark.setImageResource(this.mNoSimsIcon);
                }
            }
            View view = this.mNoSimsCombo;
            if (this.mNoSimsVisible && !this.mIsAirplaneMode) {
                i = 0;
            }
            view.setVisibility(i);
        }
    }

    /* access modifiers changed from: protected */
    public void computeCapacity() {
        if (this.mSimpleDualMobileEnable) {
            this.mSignalSimpleDualShowing = false;
            if (this.mNoSimsVisible || this.mIsAirplaneMode || this.mWifiVisible) {
                this.mSignalSimpleDualMobileContainer.setVisibility(8);
                if (this.mWifiVisible) {
                    setMobile(true);
                    if (Constants.IS_INTERNATIONAL && this.mNotchEar) {
                        Iterator<PhoneState> it = this.mPhoneStates.iterator();
                        while (it.hasNext()) {
                            it.next().setIsImsRegisted(false);
                        }
                    }
                }
                return;
            }
            refreshVolteIcons();
            if (needShowSignalSimpleDualMobile()) {
                this.mSignalSimpleDualMobileContainer.setVisibility(0);
                setMobile(false);
                refreshSignalSimpleDualMobileIcons();
                this.mSignalSimpleDualShowing = true;
                fixMobileTypeSpace();
            } else {
                this.mSignalSimpleDualMobileContainer.setVisibility(8);
                setMobile(true);
            }
        }
    }

    private boolean needShowSignalSimpleDualMobile() {
        return getMobileVisibleCount() > 1 && getVolteCount() >= 1 && Constants.IS_INTERNATIONAL;
    }

    private int getVolteCount() {
        if (this.mHideVolte) {
            return 0;
        }
        int count = 0;
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            if (it.next().mIsImsRegistered) {
                count++;
            }
        }
        return count;
    }

    private void refreshSignalSimpleDualMobileIcons() {
        for (int i = 0; i < this.mPhoneStates.size(); i++) {
            if (this.mPhoneStates.get(i).mSlot == 0) {
                updateIcon(this.mSignalSimpleDualMobile1, Icons.getSignalHalfId(Integer.valueOf(this.mPhoneStates.get(i).mMobileStrengthId)));
            } else {
                updateIcon(this.mSignalSimpleDualMobile2, Icons.getSignalHalfId(Integer.valueOf(this.mPhoneStates.get(i).mMobileStrengthId)));
            }
        }
    }

    private void refreshVolteIcons() {
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState state = it.next();
            state.setIsImsRegisted(state.mIsImsRegistered);
        }
    }

    private void fixMobileTypeSpace() {
        if (this.mPhoneStates.size() != 0) {
            this.mPhoneStates.get(0).mMobileGroup.setPadding(0, 0, (!this.mPhoneStates.get(0).mIsImsRegistered || this.mHideVolte) ? getMobileTypeSpace() : 0, 0);
        }
    }

    private int getMobileVisibleCount() {
        int mobileVisibleCount = 0;
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            if (it.next().mMobileVisible) {
                mobileVisibleCount++;
            }
        }
        return mobileVisibleCount;
    }

    private int getMobileTypeSpace() {
        if (this.mMobileTypeSpace == 0) {
            this.mMobileTypeSpace = (int) TypedValue.applyDimension(1, 3.6f, this.mContext.getResources().getDisplayMetrics());
        }
        return this.mMobileTypeSpace;
    }

    private void setIconForView(ImageView imageView, int iconId) {
        setScaledIcon(imageView, imageView.getContext().getDrawable(iconId));
    }

    private void setScaledIcon(ImageView imageView, Drawable icon) {
        if (this.mIconScaleFactor == 1.0f) {
            imageView.setImageDrawable(icon);
        } else {
            imageView.setImageDrawable(new ScalingDrawableWrapper(icon, this.mIconScaleFactor));
        }
    }

    private void setBadgedWifiIconForView(ImageView imageView, int wifiPieId, int badgeId) {
        LayerDrawable icon = new LayerDrawable(new Drawable[]{imageView.getContext().getDrawable(wifiPieId), imageView.getContext().getDrawable(badgeId)});
        icon.mutate().setTint(getColorAttr(imageView.getContext(), R.attr.singleToneColor));
        setScaledIcon(imageView, icon);
    }

    private static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, -1);
        ta.recycle();
        return colorAccent;
    }

    public void onDarkChanged(Rect tintArea, float darkIntensity, int tint) {
        boolean changed = darkIntensity != this.mDarkIntensity || !this.mTintArea.equals(tintArea);
        this.mDarkIntensity = darkIntensity;
        this.mTintArea.set(tintArea);
        if (changed && isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private void applyIconTint() {
        updateIcon(this.mAirplane, R.drawable.stat_sys_signal_flightmode);
        applyDarkIntensity(DarkIconDispatcherHelper.getDarkIntensity(this.mTintArea, this.mNoSims, this.mDarkIntensity), this.mNoSims, this.mNoSimsDark);
        updateIcon(this.mWifi, this.mDemoMode ? R.drawable.stat_sys_wifi_signal_4 : this.mWifiStrengthId);
        updateIcon(this.mWifiApConnectMark, R.drawable.stat_sys_wifi_ap);
        updateIcon(this.mWifiAp, R.drawable.stat_sys_wifi_ap_on);
        int i = 0;
        updateIcon(this.mVowifi[0], R.drawable.stat_sys_vowifi);
        updateIcon(this.mVowifi[1], R.drawable.stat_sys_vowifi);
        updateIcon(this.mVpn, R.drawable.stat_sys_vpn);
        setTextColor(this.mWifiLabel);
        this.mWifiActivity.setImageResource(Icons.get(Integer.valueOf(this.mLastWifiActivityId), DarkIconDispatcherHelper.inDarkMode(this.mTintArea, this.mWifiActivity, this.mDarkIntensity)));
        applyDarkIntensity(this.mDarkIntensity, this.mEthernet, this.mEthernetDark);
        this.mDemoMobileSignal.setImageResource(Icons.get(Integer.valueOf(R.drawable.stat_sys_signal_5), DarkIconDispatcherHelper.inDarkMode(this.mTintArea, this.mDemoMobileSignal, this.mDarkIntensity)));
        while (true) {
            int i2 = i;
            if (i2 < this.mPhoneStates.size()) {
                this.mPhoneStates.get(i2).setIconTint(this.mIconTint, this.mDarkIntensity, this.mTintArea);
                if (this.mSimpleDualMobileEnable) {
                    if (this.mPhoneStates.get(i2).mSlot == 0) {
                        updateIcon(this.mSignalSimpleDualMobile1, Icons.getSignalHalfId(Integer.valueOf(this.mPhoneStates.get(i2).mMobileStrengthId)));
                    } else {
                        updateIcon(this.mSignalSimpleDualMobile2, Icons.getSignalHalfId(Integer.valueOf(this.mPhoneStates.get(i2).mMobileStrengthId)));
                    }
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private void applyDarkIntensity(float darkIntensity, View lightIcon, View darkIcon) {
        float f = 0.0f;
        boolean isDarkMode = darkIntensity > 0.0f;
        Drawable drawable = ((ImageView) darkIcon).getDrawable();
        if (drawable != null) {
            if (!isDarkMode || !Util.showCtsSpecifiedColor()) {
                drawable.setColorFilter(null);
            } else {
                if (sFilterColor == 0) {
                    sFilterColor = this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts);
                }
                drawable.setColorFilter(sFilterColor, PorterDuff.Mode.SRC_IN);
            }
        }
        lightIcon.setAlpha(isDarkMode ? 0.0f : 1.0f);
        if (isDarkMode) {
            f = 1.0f;
        }
        darkIcon.setAlpha(f);
    }

    private String getWifiName(String rssi) {
        if (rssi == null) {
            return this.mContext.getString(R.string.status_bar_settings_signal_meter_wifi_nossid);
        }
        return removeDoubleQuotes(rssi);
    }

    private String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    /* access modifiers changed from: protected */
    public void updateIcon(ImageView icon, int drawableId) {
        boolean darkMode = false;
        if (drawableId == 0) {
            icon.setImageResource(0);
            return;
        }
        if (DarkIconDispatcherHelper.getDarkIntensity(this.mTintArea, icon, this.mDarkIntensity) > 0.0f) {
            darkMode = true;
        }
        icon.setImageResource(Icons.get(Integer.valueOf(drawableId), darkMode));
        Drawable drawable = icon.getDrawable();
        if (drawable == null) {
            return;
        }
        if (!darkMode || !Util.showCtsSpecifiedColor()) {
            drawable.setColorFilter(null);
            return;
        }
        if (sFilterColor == 0) {
            sFilterColor = this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts);
        }
        drawable.setColorFilter(sFilterColor, PorterDuff.Mode.SRC_IN);
    }

    public void setTextColor(TextView textView) {
        boolean showCtsSpecifiedColor = Util.showCtsSpecifiedColor();
        int i = R.color.status_bar_textColor;
        boolean z = false;
        if (showCtsSpecifiedColor) {
            if (DarkIconDispatcherHelper.getDarkIntensity(this.mTintArea, textView, this.mDarkIntensity) > 0.0f) {
                z = true;
            }
            boolean isDark = z;
            Resources resources = this.mContext.getResources();
            if (isDark) {
                i = R.color.status_bar_icon_text_color_dark_mode_cts;
            }
            textView.setTextColor(resources.getColor(i));
            return;
        }
        if (DarkIconDispatcherHelper.getDarkIntensity(this.mTintArea, textView, this.mDarkIntensity) > 0.0f) {
            z = true;
        }
        boolean isDark2 = z;
        Resources resources2 = this.mContext.getResources();
        if (isDark2) {
            i = R.color.status_bar_textColor_darkmode;
        }
        textView.setTextColor(resources2.getColor(i));
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (command.equals("enter")) {
            this.mDemoMode = true;
            apply();
        } else if (command.equals("exit")) {
            this.mDemoMode = false;
            apply();
        }
    }

    /* access modifiers changed from: protected */
    public boolean isNotSignalSimpleDualShowing() {
        return !this.mSignalSimpleDualShowing;
    }

    /* access modifiers changed from: protected */
    public boolean couldHideVolte() {
        return Constants.IS_INTERNATIONAL && !this.mSimpleDualMobileEnable;
    }

    /* access modifiers changed from: private */
    public boolean notchEarDualEnable() {
        return this.mNotchEarDualEnable;
    }

    private boolean ethernetEnable() {
        return this.mEthernetAble;
    }

    /* access modifiers changed from: private */
    public boolean isBuildTest() {
        return Build.IS_CM_CUSTOMIZATION_TEST;
    }

    /* access modifiers changed from: private */
    public boolean isCUDripBuildTEST() {
        return Build.IS_CU_CUSTOMIZATION_TEST && this.mSimpleDualMobileEnable;
    }
}
