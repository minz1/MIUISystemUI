package com.android.systemui.usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.systemui.R;

public class UsbPermissionActivity extends AlertActivity implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private UsbAccessory mAccessory;
    private CheckBox mAlwaysUse;
    private TextView mClearDefaultHint;
    private UsbDevice mDevice;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private String mPackageName;
    private PendingIntent mPendingIntent;
    private boolean mPermissionGranted;
    private int mUid;

    /* JADX WARNING: type inference failed for: r8v0, types: [android.content.DialogInterface$OnClickListener, com.android.internal.app.AlertActivity, com.android.systemui.usb.UsbPermissionActivity, android.widget.CompoundButton$OnCheckedChangeListener, android.app.Activity] */
    public void onCreate(Bundle icicle) {
        UsbPermissionActivity.super.onCreate(icicle);
        Intent intent = getIntent();
        this.mDevice = (UsbDevice) intent.getParcelableExtra("device");
        this.mAccessory = (UsbAccessory) intent.getParcelableExtra("accessory");
        this.mPendingIntent = (PendingIntent) intent.getParcelableExtra("android.intent.extra.INTENT");
        this.mUid = intent.getIntExtra("android.intent.extra.UID", -1);
        this.mPackageName = intent.getStringExtra("package");
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo aInfo = packageManager.getApplicationInfo(this.mPackageName, 0);
            String appName = aInfo.loadLabel(packageManager).toString();
            AlertController.AlertParams ap = this.mAlertParams;
            ap.mIcon = aInfo.loadIcon(packageManager);
            ap.mTitle = appName;
            if (this.mDevice == null) {
                ap.mMessage = getString(R.string.usb_accessory_permission_prompt, new Object[]{appName});
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
            } else {
                ap.mMessage = getString(R.string.usb_device_permission_prompt, new Object[]{appName});
                this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
            }
            ap.mPositiveButtonText = getString(17039370);
            ap.mNegativeButtonText = getString(17039360);
            ap.mPositiveButtonListener = this;
            ap.mNegativeButtonListener = this;
            ap.mView = ((LayoutInflater) getSystemService("layout_inflater")).inflate(17367090, null);
            this.mAlwaysUse = (CheckBox) ap.mView.findViewById(16908713);
            if (this.mDevice == null) {
                this.mAlwaysUse.setText(R.string.always_use_accessory);
            } else {
                this.mAlwaysUse.setText(R.string.always_use_device);
            }
            this.mAlwaysUse.setOnCheckedChangeListener(this);
            this.mClearDefaultHint = (TextView) ap.mView.findViewById(16908804);
            this.mClearDefaultHint.setVisibility(8);
            setupAlert();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UsbPermissionActivity", "unable to look up package name", e);
            finish();
        }
    }

    /* JADX WARNING: type inference failed for: r6v0, types: [android.content.Context, com.android.internal.app.AlertActivity, com.android.systemui.usb.UsbPermissionActivity] */
    public void onDestroy() {
        IUsbManager service = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
        Intent intent = new Intent();
        try {
            if (this.mDevice != null) {
                intent.putExtra("device", this.mDevice);
                if (this.mPermissionGranted) {
                    service.grantDevicePermission(this.mDevice, this.mUid);
                    if (this.mAlwaysUse.isChecked()) {
                        service.setDevicePackage(this.mDevice, this.mPackageName, UserHandle.getUserId(this.mUid));
                    }
                }
            }
            if (this.mAccessory != null) {
                intent.putExtra("accessory", this.mAccessory);
                if (this.mPermissionGranted) {
                    service.grantAccessoryPermission(this.mAccessory, this.mUid);
                    if (this.mAlwaysUse.isChecked()) {
                        service.setAccessoryPackage(this.mAccessory, this.mPackageName, UserHandle.getUserId(this.mUid));
                    }
                }
            }
            intent.putExtra("permission", this.mPermissionGranted);
            this.mPendingIntent.send(this, 0, intent);
        } catch (PendingIntent.CanceledException e) {
            Log.w("UsbPermissionActivity", "PendingIntent was cancelled");
        } catch (RemoteException e2) {
            Log.e("UsbPermissionActivity", "IUsbService connection failed", e2);
        }
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        UsbPermissionActivity.super.onDestroy();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            this.mPermissionGranted = true;
        }
        finish();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mClearDefaultHint != null) {
            if (isChecked) {
                this.mClearDefaultHint.setVisibility(0);
            } else {
                this.mClearDefaultHint.setVisibility(8);
            }
        }
    }
}
