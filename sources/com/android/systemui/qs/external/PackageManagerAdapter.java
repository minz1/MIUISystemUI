package com.android.systemui.qs.external;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import com.android.systemui.content.pm.PackageManagerCompat;

public class PackageManagerAdapter {
    private IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    private PackageManager mPackageManager;

    public PackageManagerAdapter(Context context) {
        this.mPackageManager = context.getPackageManager();
    }

    public ServiceInfo getServiceInfo(ComponentName className, int flags, int userId) throws RemoteException {
        return this.mIPackageManager.getServiceInfo(className, flags, userId);
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException {
        return this.mPackageManager.getServiceInfo(component, flags);
    }

    public PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId) throws PackageManager.NameNotFoundException {
        return PackageManagerCompat.getPackageInfoAsUser(this.mPackageManager, packageName, flags, userId);
    }
}
