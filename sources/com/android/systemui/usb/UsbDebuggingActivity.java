package com.android.systemui.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.systemui.plugins.R;
import miui.app.Activity;
import miui.app.AlertDialog;

public class UsbDebuggingActivity extends Activity {
    /* access modifiers changed from: private */
    public AlertDialog mCheckBoxDialog;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    /* access modifiers changed from: private */
    public String mKey;
    private DialogInterface.OnClickListener onClickListener;
    private DialogInterface.OnDismissListener onDismissListener;

    private class UsbDisconnectedReceiver extends BroadcastReceiver {
        private final Activity mActivity;

        public UsbDisconnectedReceiver(Activity activity) {
            this.mActivity = activity;
        }

        public void onReceive(Context content, Intent intent) {
            if ("android.hardware.usb.action.USB_STATE".equals(intent.getAction()) && !intent.getBooleanExtra("connected", false)) {
                this.mActivity.finish();
            }
        }
    }

    /* JADX WARNING: type inference failed for: r8v0, types: [android.content.Context, com.android.systemui.usb.UsbDebuggingActivity, miui.app.Activity] */
    public void onCreate(Bundle icicle) {
        UsbDebuggingActivity.super.onCreate(icicle);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        getWindow().getDecorView().setAlpha(0.0f);
        if (SystemProperties.getInt("service.adb.tcp.port", 0) == 0) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver(this);
        }
        Intent intent = getIntent();
        String fingerprints = intent.getStringExtra("fingerprints");
        this.mKey = intent.getStringExtra("key");
        if (fingerprints == null || this.mKey == null) {
            finish();
            return;
        }
        this.onClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                boolean alwaysAllow = false;
                boolean allow = which == -1;
                if (allow && UsbDebuggingActivity.this.mCheckBoxDialog.isChecked()) {
                    alwaysAllow = true;
                }
                try {
                    IUsbManager service = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
                    if (allow) {
                        service.allowUsbDebugging(alwaysAllow, UsbDebuggingActivity.this.mKey);
                    } else {
                        service.denyUsbDebugging();
                    }
                } catch (Exception e) {
                    Log.e("UsbDebuggingActivity", "Unable to notify Usb service", e);
                }
                UsbDebuggingActivity.this.finish();
            }
        };
        this.onDismissListener = new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                UsbDebuggingActivity.this.finish();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_Dialog_Alert);
        builder.setTitle(getString(com.android.systemui.R.string.usb_debugging_title)).setMessage(getString(com.android.systemui.R.string.usb_debugging_message, new Object[]{fingerprints})).setCheckBox(true, getString(com.android.systemui.R.string.usb_debugging_always)).setCancelable(true).setPositiveButton(getString(17039370), this.onClickListener).setNegativeButton(getString(17039360), this.onClickListener).setOnDismissListener(this.onDismissListener);
        this.mCheckBoxDialog = builder.create();
        this.mCheckBoxDialog.show();
    }

    public void onStart() {
        UsbDebuggingActivity.super.onStart();
        registerReceiver(this.mDisconnectedReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        UsbDebuggingActivity.super.onStop();
    }
}
