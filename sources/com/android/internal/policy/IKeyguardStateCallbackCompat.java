package com.android.internal.policy;

import android.os.IBinder;
import android.os.RemoteException;

public class IKeyguardStateCallbackCompat {
    private IKeyguardStateCallback mCallback;

    public IKeyguardStateCallbackCompat(IKeyguardStateCallback callback) {
        this.mCallback = callback;
    }

    public void onShowingStateChanged(boolean showing) throws RemoteException {
        this.mCallback.onShowingStateChanged(showing);
    }

    public void onSimSecureStateChanged(boolean simSecure) throws RemoteException {
        this.mCallback.onSimSecureStateChanged(simSecure);
    }

    public void onInputRestrictedStateChanged(boolean inputRestricted) throws RemoteException {
        this.mCallback.onInputRestrictedStateChanged(inputRestricted);
    }

    public void onTrustedChanged(boolean trusted) throws RemoteException {
        this.mCallback.onTrustedChanged(trusted);
    }

    public void onHasLockscreenWallpaperChanged(boolean hasLockscreenWallpaper) throws RemoteException {
        this.mCallback.onHasLockscreenWallpaperChanged(hasLockscreenWallpaper);
    }

    public IBinder asBinder() {
        return this.mCallback.asBinder();
    }
}
