package com.android.systemui.content.pm;

import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import java.util.List;

public class PackageManagerCompat {
    public static PackageInfo getPackageInfoAsUser(PackageManager pm, String packageName, int flags, int userId) throws PackageManager.NameNotFoundException {
        return pm.getPackageInfoAsUser(packageName, flags, userId);
    }

    public static boolean hasSystemFeature(IPackageManager packageManager, String name, int version) throws RemoteException {
        return packageManager.hasSystemFeature(name, version);
    }

    public static List<ResolveInfo> queryBroadcastReceiversAsUser(PackageManager pm, Intent intent, int flags, int userId) {
        return pm.queryBroadcastReceiversAsUser(intent, flags, -2);
    }
}
