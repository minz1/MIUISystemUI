package android.view;

import android.os.IBinder;
import android.os.RemoteException;

public class IWindowManagerCompat {
    public static void addWindowToken(IWindowManager windowManager, IBinder token, int type, int displayId) throws RemoteException {
        windowManager.addWindowToken(token, type, displayId);
    }

    public static void removeWindowToken(IWindowManager windowManager, IBinder token, int displayId) throws RemoteException {
        windowManager.removeWindowToken(token, displayId);
    }
}
