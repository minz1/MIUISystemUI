package android.support.v4.media;

import android.support.v4.media.MediaSessionManager;
import android.support.v4.util.ObjectsCompat;
import android.text.TextUtils;

class MediaSessionManagerImplBase {
    private static final boolean DEBUG = MediaSessionManager.DEBUG;

    static class RemoteUserInfo implements MediaSessionManager.RemoteUserInfoImpl {
        private String mPackageName;
        private int mPid;
        private int mUid;

        RemoteUserInfo(String packageName, int pid, int uid) {
            this.mPackageName = packageName;
            this.mPid = pid;
            this.mUid = uid;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof RemoteUserInfo)) {
                return false;
            }
            RemoteUserInfo otherUserInfo = (RemoteUserInfo) obj;
            if (TextUtils.equals(this.mPackageName, otherUserInfo.mPackageName) && this.mPid == otherUserInfo.mPid && this.mUid == otherUserInfo.mUid) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return ObjectsCompat.hash(this.mPackageName, Integer.valueOf(this.mPid), Integer.valueOf(this.mUid));
        }
    }
}
