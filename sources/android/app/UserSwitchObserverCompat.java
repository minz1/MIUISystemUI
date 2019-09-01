package android.app;

import android.app.IUserSwitchObserver;
import android.os.IRemoteCallback;
import android.os.RemoteException;

public class UserSwitchObserverCompat extends IUserSwitchObserver.Stub {
    public void onUserSwitching(int newUserId, IRemoteCallback reply) throws RemoteException {
        if (reply != null) {
            reply.sendResult(null);
        }
    }

    public void onUserSwitchComplete(int newUserId) throws RemoteException {
    }

    public void onForegroundProfileSwitch(int newProfileId) throws RemoteException {
    }

    public void onLockedBootComplete(int newUserId) throws RemoteException {
    }
}
