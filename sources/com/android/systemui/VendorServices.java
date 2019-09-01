package com.android.systemui;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.IWindowManager;
import com.android.systemui.vendor.HeadsetPolicy;
import com.android.systemui.vendor.OrientationPolicy;

public class VendorServices extends SystemUI {
    public void start() {
        new HeadsetPolicy(this.mContext);
        boolean bHasNavigationBar = false;
        try {
            bHasNavigationBar = IWindowManager.Stub.asInterface(ServiceManager.getService("window")).hasNavigationBar();
        } catch (RemoteException e) {
        }
        if (bHasNavigationBar) {
            new OrientationPolicy(this.mContext);
        }
    }
}
