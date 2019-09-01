package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccessPointControllerImpl implements WifiTracker.WifiListener, NetworkController.AccessPointController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("AccessPointController", 3);
    private static final int[] ICONS = {R.drawable.ic_qs_wifi_full_0, R.drawable.ic_qs_wifi_full_1, R.drawable.ic_qs_wifi_full_2, R.drawable.ic_qs_wifi_full_3, R.drawable.ic_qs_wifi_full_4};
    private final ArrayList<NetworkController.AccessPointController.AccessPointCallback> mCallbacks = new ArrayList<>();
    private final WifiManager.ActionListener mConnectListener = new WifiManager.ActionListener() {
        public void onSuccess() {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect success");
            }
        }

        public void onFailure(int reason) {
            if (AccessPointControllerImpl.DEBUG) {
                Log.d("AccessPointController", "connect failure reason=" + reason);
            }
        }
    };
    private final Context mContext;
    private int mCurrentUser;
    private final UserManager mUserManager;
    private final WifiTracker mWifiTracker;

    public AccessPointControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mWifiTracker = new WifiTracker(context, this, false, true);
        this.mCurrentUser = ActivityManager.getCurrentUser();
    }

    /* access modifiers changed from: protected */
    public void finalize() throws Throwable {
        super.finalize();
        this.mWifiTracker.onDestroy();
    }

    public boolean canConfigWifi() {
        return !this.mUserManager.hasUserRestriction("no_config_wifi", new UserHandle(this.mCurrentUser));
    }

    public void onUserSwitched(int newUserId) {
        this.mCurrentUser = newUserId;
    }

    public void addAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback callback) {
        if (callback != null && !this.mCallbacks.contains(callback)) {
            if (DEBUG) {
                Log.d("AccessPointController", "addCallback " + callback);
            }
            this.mCallbacks.add(callback);
            if (this.mCallbacks.size() == 1) {
                this.mWifiTracker.onStart();
            }
        }
    }

    public void removeAccessPointCallback(NetworkController.AccessPointController.AccessPointCallback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("AccessPointController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
            if (this.mCallbacks.isEmpty()) {
                this.mWifiTracker.onStop();
            }
        }
    }

    public void scanForAccessPoints() {
        fireAcccessPointsCallback(this.mWifiTracker.getAccessPoints());
    }

    public int getIcon(AccessPoint ap) {
        int level = ap.getLevel();
        return ICONS[level >= 0 ? level : 0];
    }

    public boolean connect(AccessPoint ap) {
        if (ap == null) {
            return false;
        }
        if (DEBUG) {
            Log.d("AccessPointController", "connect networkId=" + ap.getConfig().networkId);
        }
        if (ap.isSaved()) {
            this.mWifiTracker.getManager().connect(ap.getConfig().networkId, this.mConnectListener);
        } else if (ap.getSecurity() != 0) {
            Intent intent = new Intent("android.settings.WIFI_SETTINGS");
            intent.putExtra("wifi_start_connect_ssid", ap.getSsidStr());
            intent.putExtra("ssid", ap.getSsidStr());
            intent.addFlags(268435456);
            fireSettingsIntentCallback(intent);
            return true;
        } else {
            ap.generateOpenNetworkConfig();
            this.mWifiTracker.getManager().connect(ap.getConfig(), this.mConnectListener);
        }
        return false;
    }

    private void fireSettingsIntentCallback(Intent intent) {
        Iterator<NetworkController.AccessPointController.AccessPointCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onSettingsActivityTriggered(intent);
        }
    }

    private void fireAcccessPointsCallback(List<AccessPoint> aps) {
        Iterator<NetworkController.AccessPointController.AccessPointCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onAccessPointsChanged(aps);
        }
    }

    public void dump(PrintWriter pw) {
        this.mWifiTracker.dump(pw);
    }

    public void onWifiStateChanged(int state) {
    }

    public void onConnectedChanged() {
        fireAcccessPointsCallback(this.mWifiTracker.getAccessPoints());
    }

    public void onAccessPointsChanged() {
        fireAcccessPointsCallback(this.mWifiTracker.getAccessPoints());
    }

    public void updateVerboseLoggingLevel() {
        WifiManager wifiManager = this.mWifiTracker.getManager();
        if (wifiManager != null) {
            WifiTracker wifiTracker = this.mWifiTracker;
            WifiTracker.sVerboseLogging = wifiManager.getVerboseLoggingLevel() > 0;
        }
    }
}
