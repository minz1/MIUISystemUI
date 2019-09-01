package android.support.v4.media;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.IMediaSession2;
import android.support.v4.media.SessionToken2;
import android.text.TextUtils;

final class SessionToken2ImplBase implements SessionToken2.SupportLibraryImpl {
    private final ComponentName mComponentName;
    private final IMediaSession2 mISession2;
    private final String mPackageName;
    private final String mServiceName;
    private final String mSessionId;
    private final int mType;
    private final int mUid;

    SessionToken2ImplBase(int uid, int type, String packageName, String serviceName, String sessionId, IMediaSession2 iSession2) {
        this.mUid = uid;
        this.mType = type;
        this.mPackageName = packageName;
        this.mServiceName = serviceName;
        this.mComponentName = this.mType == 0 ? null : new ComponentName(packageName, serviceName);
        this.mSessionId = sessionId;
        this.mISession2 = iSession2;
    }

    public int hashCode() {
        return this.mType + (31 * (this.mUid + ((this.mPackageName.hashCode() + ((this.mSessionId.hashCode() + ((this.mServiceName != null ? this.mServiceName.hashCode() : 0) * 31)) * 31)) * 31)));
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof SessionToken2ImplBase)) {
            return false;
        }
        SessionToken2ImplBase other = (SessionToken2ImplBase) obj;
        if (this.mUid == other.mUid && TextUtils.equals(this.mPackageName, other.mPackageName) && TextUtils.equals(this.mServiceName, other.mServiceName) && TextUtils.equals(this.mSessionId, other.mSessionId) && this.mType == other.mType && sessionBinderEquals(this.mISession2, other.mISession2)) {
            z = true;
        }
        return z;
    }

    private boolean sessionBinderEquals(IMediaSession2 a, IMediaSession2 b) {
        if (a != null && b != null) {
            return a.asBinder().equals(b.asBinder());
        }
        return a == b;
    }

    public String toString() {
        return "SessionToken {pkg=" + this.mPackageName + " id=" + this.mSessionId + " type=" + this.mType + " service=" + this.mServiceName + " IMediaSession2=" + this.mISession2 + "}";
    }

    public static SessionToken2ImplBase fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int uid = bundle.getInt("android.media.token.uid");
        int type = bundle.getInt("android.media.token.type", -1);
        String packageName = bundle.getString("android.media.token.package_name");
        String serviceName = bundle.getString("android.media.token.service_name");
        String sessionId = bundle.getString("android.media.token.session_id");
        IMediaSession2 iSession2 = IMediaSession2.Stub.asInterface(BundleCompat.getBinder(bundle, "android.media.token.session_binder"));
        switch (type) {
            case 0:
                if (iSession2 == null) {
                    throw new IllegalArgumentException("Unexpected token for session, binder=" + iSession2);
                }
                break;
            case 1:
            case 2:
                if (TextUtils.isEmpty(serviceName)) {
                    throw new IllegalArgumentException("Session service needs service name");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid type");
        }
        if (TextUtils.isEmpty(packageName) || sessionId == null) {
            throw new IllegalArgumentException("Package name nor ID cannot be null.");
        }
        SessionToken2ImplBase sessionToken2ImplBase = new SessionToken2ImplBase(uid, type, packageName, serviceName, sessionId, iSession2);
        return sessionToken2ImplBase;
    }
}
