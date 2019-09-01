package com.android.keyguard;

import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;

public class WallpaperManagerCompat {
    public static WallpaperInfo getWallpaperInfo(Context context) {
        try {
            return IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper")).getWallpaperInfo(KeyguardUpdateMonitor.getCurrentUser());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }
}
