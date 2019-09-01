package com.android.systemui.miui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.Dependency;

public class WifiLabelText extends TextView {
    private final BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public String mCustomCarrier;
    private ContentObserver mCustomCarrierObserver;
    /* access modifiers changed from: private */
    public boolean mForceHide;
    /* access modifiers changed from: private */
    public String mRealCarrier;
    /* access modifiers changed from: private */
    public boolean mShowCarrier;
    private ContentObserver mShowCarrierObserver;
    private boolean mSupportNetwork;

    public WifiLabelText(Context context) {
        this(context, null);
    }

    public WifiLabelText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRealCarrier = "";
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    WifiLabelText.this.initCarrier();
                }
            }
        };
        this.mShowCarrierObserver = new ContentObserver(new Handler((Looper) Dependency.get(Dependency.BG_LOOPER))) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                WifiLabelText wifiLabelText = WifiLabelText.this;
                boolean z = true;
                if (Settings.System.getIntForUser(WifiLabelText.this.mContext.getContentResolver(), "status_bar_show_carrier_under_keyguard", 1, -2) != 1) {
                    z = false;
                }
                boolean unused = wifiLabelText.mShowCarrier = z;
                WifiLabelText.this.updateCarrier();
            }
        };
    }

    public boolean isFocused() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
            this.mSupportNetwork = true;
            setText("");
            setVisibility(8);
            return;
        }
        this.mSupportNetwork = false;
        initCarrier();
        registerObservers();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private void registerObservers() {
        this.mCustomCarrierObserver = new ContentObserver(new Handler((Looper) Dependency.get(Dependency.BG_LOOPER))) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                String unused = WifiLabelText.this.mCustomCarrier = MiuiSettings.System.getStringForUser(WifiLabelText.this.mContext.getContentResolver(), "status_bar_custom_carrier0", -2);
                WifiLabelText.this.updateCarrier();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_custom_carrier0"), false, this.mCustomCarrierObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_carrier_under_keyguard"), false, this.mShowCarrierObserver, -1);
        updateCarrier();
    }

    /* access modifiers changed from: private */
    public void initCarrier() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "status_bar_show_carrier_under_keyguard", 1, -2) != 1) {
            z = false;
        }
        this.mShowCarrier = z;
        this.mCustomCarrier = MiuiSettings.System.getStringForUser(this.mContext.getContentResolver(), "status_bar_custom_carrier0", -2);
        updateCarrier();
    }

    public void unregisterObservers() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mCustomCarrierObserver);
        this.mContext.getContentResolver().unregisterContentObserver(this.mShowCarrierObserver);
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!this.mSupportNetwork) {
            unregisterObservers();
        }
    }

    public void updateCarrier() {
        if (!this.mSupportNetwork) {
            post(new Runnable() {
                public void run() {
                    String carrier;
                    if (!TextUtils.isEmpty(WifiLabelText.this.mCustomCarrier)) {
                        carrier = WifiLabelText.this.mCustomCarrier;
                    } else {
                        carrier = WifiLabelText.this.mRealCarrier;
                    }
                    if (!TextUtils.isEmpty(carrier) && !carrier.equals(WifiLabelText.this.getText())) {
                        WifiLabelText.this.setText(carrier);
                    }
                    WifiLabelText.this.setVisibility((WifiLabelText.this.mForceHide || !WifiLabelText.this.mShowCarrier) ? 8 : 0);
                }
            });
        }
    }

    public void setWifiLabel(boolean forceHide, String wifiLabel) {
        if (this.mSupportNetwork) {
            return;
        }
        if ((!TextUtils.isEmpty(wifiLabel) && !wifiLabel.equals(this.mRealCarrier)) || this.mForceHide != forceHide) {
            this.mRealCarrier = wifiLabel;
            this.mForceHide = forceHide;
            updateCarrier();
        }
    }
}
