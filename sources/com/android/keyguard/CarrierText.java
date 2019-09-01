package com.android.keyguard;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.List;
import miui.os.Build;

public class CarrierText extends TextView implements DarkIconDispatcher.DarkReceiver, NetworkController.CarrierNameListener, NetworkController.EmergencyListener, NetworkController.MobileTypeListener {
    private static CharSequence mSeparator;
    /* access modifiers changed from: private */
    public boolean mAirplaneModeOn;
    private final BroadcastReceiver mBroadcastReceiver;
    private KeyguardUpdateMonitorCallback mCallback;
    /* access modifiers changed from: private */
    public String[] mCustomCarrier;
    private ContentObserver[] mCustomCarrierObserver;
    private boolean mEmergencyOnly;
    /* access modifiers changed from: private */
    public boolean mForceHide;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private String[] mMobileType;
    /* access modifiers changed from: private */
    public final int mPhoneCount;
    /* access modifiers changed from: private */
    public boolean mShowCarrier;
    private ContentObserver mShowCarrierObserver;
    /* access modifiers changed from: private */
    public int mShowStyle;
    /* access modifiers changed from: private */
    public String[] mSimCarrier;
    /* access modifiers changed from: private */
    public boolean[] mSimErrorState;
    private boolean mSupportNetwork;

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
            this.mSupportNetwork = true;
            this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
            initCarrier();
            registerObservers();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
            ((NetworkController) Dependency.get(NetworkController.class)).addCarrierNameListener(this);
            ((NetworkController) Dependency.get(NetworkController.class)).addEmergencyListener(this);
            if (isCustomizationTest()) {
                ((NetworkController) Dependency.get(NetworkController.class)).addMobileTypeListener(this);
                return;
            }
            return;
        }
        this.mSupportNetwork = false;
        this.mKeyguardUpdateMonitor = null;
        setText("");
    }

    private void registerObservers() {
        Handler handler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        for (int i = 0; i < this.mPhoneCount; i++) {
            this.mCustomCarrierObserver[i] = new ContentObserver(handler) {
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    for (int i = 0; i < CarrierText.this.mPhoneCount; i++) {
                        String[] access$800 = CarrierText.this.mCustomCarrier;
                        ContentResolver contentResolver = CarrierText.this.mContext.getContentResolver();
                        access$800[i] = MiuiSettings.System.getStringForUser(contentResolver, "status_bar_custom_carrier" + i, -2);
                    }
                    CarrierText.this.updateCarrier();
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_custom_carrier" + i), false, this.mCustomCarrierObserver[i], -1);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_carrier_under_keyguard"), false, this.mShowCarrierObserver, -1);
        updateCarrier();
    }

    /* access modifiers changed from: private */
    public void initCarrier() {
        boolean z = false;
        if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
            this.mShowCarrier = Settings.System.getIntForUser(this.mContext.getContentResolver(), "status_bar_show_carrier_under_keyguard", 1, -2) == 1;
            for (int i = 0; i < this.mPhoneCount; i++) {
                this.mCustomCarrier[i] = MiuiSettings.System.getStringForUser(this.mContext.getContentResolver(), "status_bar_custom_carrier" + i, -2);
            }
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
                z = true;
            }
            this.mAirplaneModeOn = z;
            updateCarrier();
        }
    }

    public void unregisterObservers() {
        for (int i = 0; i < this.mPhoneCount; i++) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mCustomCarrierObserver[i]);
        }
        this.mContext.getContentResolver().unregisterContentObserver(this.mShowCarrierObserver);
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    private boolean isCustomizationTest() {
        return Build.IS_CM_CUSTOMIZATION_TEST || Build.IS_CU_CUSTOMIZATION_TEST;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mSupportNetwork) {
            if (this.mKeyguardUpdateMonitor != null) {
                this.mKeyguardUpdateMonitor.removeCallback(this.mCallback);
            }
            unregisterObservers();
            ((NetworkController) Dependency.get(NetworkController.class)).removeCarrierNameListener(this);
            ((NetworkController) Dependency.get(NetworkController.class)).removeEmergencyListener(this);
            if (isCustomizationTest()) {
                ((NetworkController) Dependency.get(NetworkController.class)).removeMobileTypeListener(this);
            }
        }
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this);
    }

    public CarrierText(Context context) {
        this(context, null);
    }

    public CarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowStyle = 0;
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            public void onRefreshCarrierInfo() {
                CarrierText.this.updateCarrier();
            }

            public void onAirplaneModeChanged() {
                CarrierText carrierText = CarrierText.this;
                boolean z = true;
                if (Settings.Global.getInt(CarrierText.this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
                    z = false;
                }
                boolean unused = carrierText.mAirplaneModeOn = z;
                CarrierText.this.updateCarrier();
            }

            public void onFinishedGoingToSleep(int why) {
                CarrierText.this.setSelected(false);
            }

            public void onStartedWakingUp() {
                CarrierText.this.setSelected(true);
            }

            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                if (slotId < 0) {
                    Log.d("CarrierText", "onSimStateChanged() - slotId invalid: " + slotId);
                    return;
                }
                if (CarrierText.this.isSimErrorByIccState(simState)) {
                    CarrierText.this.mSimErrorState[slotId] = true;
                    CarrierText.this.updateCarrier();
                } else {
                    CarrierText.this.mSimErrorState[slotId] = false;
                    CarrierText.this.updateCarrier();
                }
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    CarrierText.this.initCarrier();
                }
            }
        };
        this.mShowCarrierObserver = new ContentObserver(new Handler((Looper) Dependency.get(Dependency.BG_LOOPER))) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                CarrierText carrierText = CarrierText.this;
                boolean z = true;
                if (Settings.System.getIntForUser(CarrierText.this.mContext.getContentResolver(), "status_bar_show_carrier_under_keyguard", 1, -2) != 1) {
                    z = false;
                }
                boolean unused = carrierText.mShowCarrier = z;
                CarrierText.this.updateCarrier();
            }
        };
        this.mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        this.mSimErrorState = new boolean[this.mPhoneCount];
        this.mCustomCarrierObserver = new ContentObserver[this.mPhoneCount];
        this.mCustomCarrier = new String[this.mPhoneCount];
        this.mSimCarrier = new String[this.mPhoneCount];
        this.mMobileType = new String[this.mPhoneCount];
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        setTextColor(DarkIconDispatcherHelper.getTint(area, this, tint));
    }

    public void updateCarrierName(int slotId, String carrierName) {
        if (slotId < this.mPhoneCount) {
            this.mSimCarrier[slotId] = carrierName;
        }
        updateCarrier();
    }

    public void setEmergencyCallsOnly(boolean emergencyOnly) {
        if (this.mEmergencyOnly != emergencyOnly) {
            this.mEmergencyOnly = emergencyOnly;
            updateCarrier();
        }
    }

    public void updateMobileTypeName(int slotId, String mobileTypeName) {
        if (slotId < this.mPhoneCount) {
            this.mMobileType[slotId] = mobileTypeName;
            updateCarrier();
        }
    }

    public void updateCarrier() {
        if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
            post(new Runnable() {
                public void run() {
                    String[] carrier = new String[CarrierText.this.mPhoneCount];
                    int i = 0;
                    List<SubscriptionInfo> subscriptionInfo = CarrierText.this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
                    for (int i2 = 0; i2 < CarrierText.this.mPhoneCount; i2++) {
                        if (!TelephonyManager.getDefault().hasIccCard(i2) || CarrierText.this.mSimErrorState[i2]) {
                            carrier[i2] = "";
                        } else if (!TextUtils.isEmpty(CarrierText.this.mCustomCarrier[i2])) {
                            carrier[i2] = CarrierText.this.mCustomCarrier[i2];
                        } else {
                            carrier[i2] = CarrierText.this.mSimCarrier[i2];
                        }
                    }
                    String text = CarrierText.this.getCarrierString(carrier);
                    if (!TextUtils.isEmpty(text) && !text.equals(CarrierText.this.getText())) {
                        CarrierText.this.setText(text);
                    }
                    if (CarrierText.this.mForceHide) {
                        CarrierText.this.setVisibility(8);
                        return;
                    }
                    if (CarrierText.this.mShowStyle == -1) {
                        CarrierText.this.setVisibility(8);
                    } else if (CarrierText.this.mShowStyle == 1) {
                        CarrierText.this.setVisibility(0);
                    } else if (CarrierText.this.mShowStyle == 0) {
                        CarrierText carrierText = CarrierText.this;
                        if (!CarrierText.this.mShowCarrier) {
                            i = 8;
                        }
                        carrierText.setVisibility(i);
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public String getCarrierString(String[] strAry) {
        String str;
        if (this.mAirplaneModeOn) {
            return this.mContext.getResources().getString(R.string.lock_screen_carrier_airplane_mode_on);
        }
        StringBuilder result = new StringBuilder();
        if (strAry != null) {
            for (int i = 0; i < strAry.length; i++) {
                if (!TextUtils.isEmpty(strAry[i])) {
                    if (result.length() == 0) {
                        result.append(strAry[i]);
                    } else {
                        result.append(" | ");
                        result.append(strAry[i]);
                    }
                    if (isCustomizationTest()) {
                        result.append(this.mMobileType[i]);
                    }
                }
            }
        }
        if (result.length() > 0) {
            str = result.toString();
        } else {
            str = this.mContext.getResources().getString(this.mEmergencyOnly ? R.string.lock_screen_no_sim_card_emergency_only : R.string.lock_screen_no_sim_card_no_service);
        }
        return str;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        mSeparator = getResources().getString(17040168);
        setSelected(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
    }

    /* access modifiers changed from: private */
    public boolean isSimErrorByIccState(IccCardConstants.State simState) {
        if (simState == null) {
            return false;
        }
        if ((!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned() && (simState == IccCardConstants.State.ABSENT || simState == IccCardConstants.State.PERM_DISABLED) ? IccCardConstants.State.NETWORK_LOCKED : simState) == IccCardConstants.State.READY) {
            return false;
        }
        return true;
    }

    public void setShowStyle(int showStyle) {
        this.mShowStyle = showStyle;
        updateCarrier();
    }

    public void forceHide(boolean hide) {
        if (this.mForceHide != hide) {
            this.mForceHide = hide;
            if (this.mForceHide) {
                setVisibility(8);
            } else {
                updateCarrier();
            }
        }
    }
}
