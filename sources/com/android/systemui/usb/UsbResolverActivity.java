package com.android.systemui.usb;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ResolveInfoCompat;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.util.Log;
import android.widget.CheckBox;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.app.ResolverActivity;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.Iterator;

public class UsbResolverActivity extends ResolverActivity {
    private UsbAccessory mAccessory;
    private UsbDevice mDevice;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private ResolveInfo mForwardResolveInfo;
    private Intent mOtherProfileIntent;

    /* JADX WARNING: type inference failed for: r14v0, types: [android.content.Context, com.android.systemui.usb.UsbResolverActivity, android.app.Activity] */
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra("android.intent.extra.INTENT");
        if (!(targetParcelable instanceof Intent)) {
            Log.w("UsbResolverActivity", "Target is not an intent: " + targetParcelable);
            finish();
            return;
        }
        Intent target = (Intent) targetParcelable;
        ArrayList<Parcelable> list = new ArrayList<>(intent.getParcelableArrayListExtra("rlist"));
        ArrayList<ResolveInfo> rList = new ArrayList<>(list.size());
        Iterator<Parcelable> it = list.iterator();
        while (it.hasNext()) {
            rList.add((ResolveInfo) it.next());
        }
        ArrayList arrayList = new ArrayList();
        this.mForwardResolveInfo = null;
        Iterator<ResolveInfo> iterator = rList.iterator();
        while (iterator.hasNext()) {
            ResolveInfo ri = iterator.next();
            if (ResolveInfoCompat.getComponentInfo(ri).name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE)) {
                this.mForwardResolveInfo = ri;
            } else if (UserHandle.getUserId(ri.activityInfo.applicationInfo.uid) != UserHandle.myUserId()) {
                iterator.remove();
                arrayList.add(ri);
            }
        }
        this.mDevice = (UsbDevice) target.getParcelableExtra("device");
        if (this.mDevice != null) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mDevice);
        } else {
            this.mAccessory = (UsbAccessory) target.getParcelableExtra("accessory");
            if (this.mAccessory == null) {
                Log.e("UsbResolverActivity", "no device or accessory");
                finish();
                return;
            }
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver((Activity) this, this.mAccessory);
        }
        if (this.mForwardResolveInfo != null) {
            if (arrayList.size() > 1) {
                this.mOtherProfileIntent = new Intent(intent);
                this.mOtherProfileIntent.putParcelableArrayListExtra("rlist", arrayList);
            } else {
                this.mOtherProfileIntent = new Intent(this, UsbConfirmActivity.class);
                this.mOtherProfileIntent.putExtra("rinfo", (Parcelable) arrayList.get(0));
                if (this.mDevice != null) {
                    this.mOtherProfileIntent.putExtra("device", this.mDevice);
                }
                if (this.mAccessory != null) {
                    this.mOtherProfileIntent.putExtra("accessory", this.mAccessory);
                }
            }
        }
        UsbResolverActivity.super.onCreate(savedInstanceState, target, getResources().getText(17039640), null, rList, true);
        CheckBox alwaysUse = (CheckBox) findViewById(16908713);
        if (alwaysUse != null) {
            if (this.mDevice == null) {
                alwaysUse.setText(R.string.always_use_accessory);
            } else {
                alwaysUse.setText(R.string.always_use_device);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        if (this.mDisconnectedReceiver != null) {
            unregisterReceiver(this.mDisconnectedReceiver);
        }
        UsbResolverActivity.super.onDestroy();
    }

    /* JADX WARNING: type inference failed for: r9v0, types: [com.android.systemui.usb.UsbResolverActivity, android.app.Activity] */
    /* access modifiers changed from: protected */
    public boolean onTargetSelected(ResolverActivity.TargetInfo target, boolean alwaysCheck) {
        ResolveInfo ri = target.getResolveInfo();
        if (ri == this.mForwardResolveInfo) {
            startActivityAsUser(this.mOtherProfileIntent, null, UserHandleCompat.of(this.mForwardResolveInfo.targetUserId));
            return true;
        }
        try {
            IUsbManager service = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
            int uid = ri.activityInfo.applicationInfo.uid;
            int userId = UserHandle.myUserId();
            if (this.mDevice != null) {
                service.grantDevicePermission(this.mDevice, uid);
                if (alwaysCheck) {
                    service.setDevicePackage(this.mDevice, ri.activityInfo.packageName, userId);
                } else {
                    service.setDevicePackage(this.mDevice, null, userId);
                }
            } else if (this.mAccessory != null) {
                service.grantAccessoryPermission(this.mAccessory, uid);
                if (alwaysCheck) {
                    service.setAccessoryPackage(this.mAccessory, ri.activityInfo.packageName, userId);
                } else {
                    service.setAccessoryPackage(this.mAccessory, null, userId);
                }
            }
            try {
                target.startAsUser(this, null, UserHandleCompat.of(userId));
            } catch (ActivityNotFoundException e) {
                Log.e("UsbResolverActivity", "startActivity failed", e);
            }
        } catch (RemoteException e2) {
            Log.e("UsbResolverActivity", "onIntentSelected failed", e2);
        }
        return true;
    }
}
