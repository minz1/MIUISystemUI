package android.support.v4.media;

import android.os.Build;
import android.support.v4.media.MediaSessionManagerImplApi28;
import android.support.v4.media.MediaSessionManagerImplBase;
import android.util.Log;

public final class MediaSessionManager {
    static final boolean DEBUG = Log.isLoggable("MediaSessionManager", 3);
    private static final Object sLock = new Object();

    public static final class RemoteUserInfo {
        RemoteUserInfoImpl mImpl;

        public RemoteUserInfo(String packageName, int pid, int uid) {
            if (Build.VERSION.SDK_INT >= 28) {
                this.mImpl = new MediaSessionManagerImplApi28.RemoteUserInfo(packageName, pid, uid);
            } else {
                this.mImpl = new MediaSessionManagerImplBase.RemoteUserInfo(packageName, pid, uid);
            }
        }

        public boolean equals(Object obj) {
            return this.mImpl.equals(obj);
        }

        public int hashCode() {
            return this.mImpl.hashCode();
        }
    }

    interface RemoteUserInfoImpl {
    }
}
