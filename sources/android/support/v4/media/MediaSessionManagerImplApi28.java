package android.support.v4.media;

import android.media.session.MediaSessionManager;
import android.support.v4.media.MediaSessionManager;

class MediaSessionManagerImplApi28 extends MediaSessionManagerImplApi21 {

    static final class RemoteUserInfo implements MediaSessionManager.RemoteUserInfoImpl {
        MediaSessionManager.RemoteUserInfo mObject;

        RemoteUserInfo(String packageName, int pid, int uid) {
            this.mObject = new MediaSessionManager.RemoteUserInfo(packageName, pid, uid);
        }
    }
}
