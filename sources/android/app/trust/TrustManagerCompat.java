package android.app.trust;

public class TrustManagerCompat {
    public static boolean isTrustUsuallyManaged(TrustManager tm, int userId) {
        return tm.isTrustUsuallyManaged(userId);
    }

    public static void setDeviceLockedForUser(TrustManager tm, int userId, boolean locked) {
        tm.setDeviceLockedForUser(userId, locked);
    }
}
