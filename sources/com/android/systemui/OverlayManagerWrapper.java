package com.android.systemui;

import android.content.om.IOverlayManager;
import android.os.RemoteException;
import android.os.ServiceManager;

public class OverlayManagerWrapper {
    private final IOverlayManager mOverlayManager;

    public static class OverlayInfo {
        private final boolean mEnabled;
        public final String packageName;

        public OverlayInfo(android.content.om.OverlayInfo info) {
            this.mEnabled = info.isEnabled();
            this.packageName = info.packageName;
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }
    }

    public OverlayManagerWrapper(IOverlayManager overlayManager) {
        this.mOverlayManager = overlayManager;
    }

    public OverlayManagerWrapper() {
        this(IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay")));
    }

    public OverlayInfo getOverlayInfo(String overlay, int userId) {
        OverlayInfo overlayInfo = null;
        if (this.mOverlayManager == null) {
            return null;
        }
        try {
            android.content.om.OverlayInfo info = this.mOverlayManager.getOverlayInfo(overlay, userId);
            if (info != null) {
                overlayInfo = new OverlayInfo(info);
            }
            return overlayInfo;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEnabled(String overlay, boolean enabled, int userId) {
        if (this.mOverlayManager == null) {
            return false;
        }
        try {
            return this.mOverlayManager.setEnabled(overlay, enabled, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
