package android.service.vr;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Slog;

public class IVrManagerCompat {

    public interface IVrManagerCompatCallbacks {
        void onVrStateChanged(boolean z);
    }

    public static void registerListener(final IVrManagerCompatCallbacks callbacks) {
        try {
            IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager")).registerListener(new IVrStateCallbacks.Stub() {
                public void onVrStateChanged(boolean enabled) {
                    IVrManagerCompatCallbacks.this.onVrStateChanged(enabled);
                }
            });
        } catch (RemoteException e) {
            Slog.e("StatusBar", "Failed to register VR mode state listener: " + e);
        }
    }
}
