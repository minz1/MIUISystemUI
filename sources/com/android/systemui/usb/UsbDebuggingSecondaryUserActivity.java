package com.android.systemui.usb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.systemui.R;

public class UsbDebuggingSecondaryUserActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private UsbDisconnectedReceiver mDisconnectedReceiver;

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

    /* JADX WARNING: type inference failed for: r2v0, types: [android.content.DialogInterface$OnClickListener, com.android.systemui.usb.UsbDebuggingSecondaryUserActivity, com.android.internal.app.AlertActivity, android.app.Activity] */
    public void onCreate(Bundle icicle) {
        UsbDebuggingSecondaryUserActivity.super.onCreate(icicle);
        if (SystemProperties.getInt("service.adb.tcp.port", 0) == 0) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver(this);
        }
        AlertController.AlertParams ap = this.mAlertParams;
        ap.mTitle = getString(R.string.usb_debugging_secondary_user_title);
        ap.mMessage = getString(R.string.usb_debugging_secondary_user_message);
        ap.mPositiveButtonText = getString(17039370);
        ap.mPositiveButtonListener = this;
        setupAlert();
    }

    public void onStart() {
        UsbDebuggingSecondaryUserActivity.super.onStart();
        registerReceiver(this.mDisconnectedReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        UsbDebuggingSecondaryUserActivity.super.onStop();
    }

    public void onClick(DialogInterface dialog, int which) {
        finish();
    }
}
