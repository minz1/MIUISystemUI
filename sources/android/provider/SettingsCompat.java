package android.provider;

import android.os.Build;

public class SettingsCompat {
    public static final String ACTION_VPN_SETTINGS;

    static {
        String str;
        if (Build.VERSION.SDK_INT > 23) {
            str = "android.settings.VPN_SETTINGS";
        } else {
            str = "android.net.vpn.SETTINGS";
        }
        ACTION_VPN_SETTINGS = str;
    }
}
