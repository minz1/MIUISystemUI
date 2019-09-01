package com.android.systemui;

import android.os.RemoteException;
import android.util.Log;
import android.view.IDockedStackListener;
import android.view.WindowManagerGlobal;
import com.android.systemui.util.function.Consumer;

public class DockedStackExistsListener extends IDockedStackListener.Stub {
    private final Consumer<Boolean> mCallback;

    private DockedStackExistsListener(Consumer<Boolean> callback) {
        this.mCallback = callback;
    }

    public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
    }

    public void onDockedStackExistsChanged(boolean exists) throws RemoteException {
        this.mCallback.accept(Boolean.valueOf(exists));
    }

    public void onDockedStackMinimizedChanged(boolean minimized, long animDuration, boolean isHomeStackResizable) throws RemoteException {
    }

    public void onAdjustedForImeChanged(boolean adjustedForIme, long animDuration) throws RemoteException {
    }

    public void onDockSideChanged(int newDockSide) throws RemoteException {
    }

    public static void register(Consumer<Boolean> callback) {
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new DockedStackExistsListener(callback));
        } catch (RemoteException e) {
            Log.e("DockedStackExistsListener", "Failed registering docked stack exists listener", e);
        }
    }
}
