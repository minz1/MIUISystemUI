package android.net;

import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

public class ConnectivityManagerCompat {

    public static class OnStartTetheringCallback extends ConnectivityManager.OnStartTetheringCallback {
        public void onTetheringStarted() {
        }

        public void onTetheringFailed() {
        }
    }

    public static void startTethering(ConnectivityManager cm, int type, boolean showProvisioningUi, OnStartTetheringCallback callback) {
        cm.startTethering(type, showProvisioningUi, callback);
    }

    public static void startTethering(WifiManager wifiManager) {
    }

    public static void stopTethering(ConnectivityManager cm, int type) {
        cm.stopTethering(type);
    }

    public static void stopTethering(WifiManager wifiManager) {
    }
}
