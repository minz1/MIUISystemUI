package com.android.internal.statusbar;

import android.os.RemoteException;

public class StatusBarServiceCompat {
    public static void onNotificationClick(IStatusBarService barService, String key, NotificationVisibility nv) throws RemoteException {
        barService.onNotificationClick(key, nv);
    }

    public static void onNotificationActionClick(IStatusBarService barService, String key, int actionIndex, NotificationVisibility nv) throws RemoteException {
        barService.onNotificationActionClick(key, actionIndex, nv);
    }

    public static void onNotificationClear(IStatusBarService barService, String pkg, String tag, int id, int userId, String key, int dismissalSurface, NotificationVisibility nv) throws RemoteException {
        barService.onNotificationClear(pkg, tag, id, userId, key, dismissalSurface, nv);
    }
}
