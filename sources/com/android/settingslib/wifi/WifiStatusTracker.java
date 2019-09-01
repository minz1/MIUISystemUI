package com.android.settingslib.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkKey;
import android.net.NetworkRequest;
import android.net.NetworkScoreManager;
import android.net.ScoredNetwork;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Handler;
import android.os.Looper;
import com.android.settingslib.R;
import java.util.List;

public class WifiStatusTracker extends ConnectivityManager.NetworkCallback {
    public boolean connected;
    public boolean enabled;
    public int level;
    private final WifiNetworkScoreCache.CacheListener mCacheListener = new WifiNetworkScoreCache.CacheListener(this.mHandler) {
        public void networkCacheUpdated(List<ScoredNetwork> list) {
            WifiStatusTracker.this.updateStatusLabel();
            WifiStatusTracker.this.mCallback.run();
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mCallback;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            WifiStatusTracker.this.updateStatusLabel();
            WifiStatusTracker.this.mCallback.run();
        }
    };
    private final NetworkRequest mNetworkRequest = new NetworkRequest.Builder().clearCapabilities().addCapability(15).addTransportType(1).build();
    private final NetworkScoreManager mNetworkScoreManager;
    private WifiInfo mWifiInfo;
    private final WifiManager mWifiManager;
    private final WifiNetworkScoreCache mWifiNetworkScoreCache;
    public int rssi;
    public String ssid;
    public int state;
    public String statusLabel;

    public WifiStatusTracker(Context context, WifiManager wifiManager, NetworkScoreManager networkScoreManager, ConnectivityManager connectivityManager, Runnable callback) {
        this.mContext = context;
        this.mWifiManager = wifiManager;
        this.mWifiNetworkScoreCache = new WifiNetworkScoreCache(context);
        this.mNetworkScoreManager = networkScoreManager;
        this.mConnectivityManager = connectivityManager;
        this.mCallback = callback;
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mNetworkScoreManager.registerNetworkScoreCache(1, this.mWifiNetworkScoreCache, 1);
            this.mWifiNetworkScoreCache.registerListener(this.mCacheListener);
            this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback, this.mHandler);
            return;
        }
        this.mNetworkScoreManager.unregisterNetworkScoreCache(1, this.mWifiNetworkScoreCache);
        this.mWifiNetworkScoreCache.unregisterListener();
        this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
            updateWifiState();
        } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
            updateWifiState();
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            this.connected = networkInfo != null && networkInfo.isConnected();
            this.mWifiInfo = null;
            this.ssid = null;
            if (this.connected) {
                this.mWifiInfo = this.mWifiManager.getConnectionInfo();
                if (this.mWifiInfo != null) {
                    this.ssid = getValidSsid(this.mWifiInfo);
                    updateRssi(this.mWifiInfo.getRssi());
                    maybeRequestNetworkScore();
                }
            }
            updateStatusLabel();
        } else if (action.equals("android.net.wifi.RSSI_CHANGED")) {
            updateRssi(intent.getIntExtra("newRssi", -200));
            updateStatusLabel();
        }
    }

    private void updateWifiState() {
        this.state = this.mWifiManager.getWifiState();
        this.enabled = this.state == 3;
    }

    private void updateRssi(int newRssi) {
        this.rssi = newRssi;
        this.level = MiuiWifiManager.calculateSignalLevel(this.rssi, 5);
    }

    private void maybeRequestNetworkScore() {
        NetworkKey networkKey = NetworkKey.createFromWifiInfo(this.mWifiInfo);
        if (this.mWifiNetworkScoreCache.getScoredNetwork(networkKey) == null) {
            this.mNetworkScoreManager.requestScores(new NetworkKey[]{networkKey});
        }
    }

    /* access modifiers changed from: private */
    public void updateStatusLabel() {
        NetworkCapabilities networkCapabilities = this.mConnectivityManager.getNetworkCapabilities(this.mWifiManager.getCurrentNetwork());
        if (networkCapabilities != null) {
            if (networkCapabilities.hasCapability(17)) {
                this.statusLabel = this.mContext.getString(R.string.wifi_status_sign_in_required);
                return;
            } else if (!networkCapabilities.hasCapability(16)) {
                this.statusLabel = this.mContext.getString(R.string.wifi_status_no_internet);
                return;
            }
        }
        ScoredNetwork scoredNetwork = this.mWifiNetworkScoreCache.getScoredNetwork(NetworkKey.createFromWifiInfo(this.mWifiInfo));
        this.statusLabel = scoredNetwork == null ? null : AccessPoint.getSpeedLabel(this.mContext, scoredNetwork, this.rssi);
    }

    private String getValidSsid(WifiInfo info) {
        String ssid2 = info.getSSID();
        if (ssid2 != null && !"<unknown ssid>".equals(ssid2)) {
            return ssid2;
        }
        List<WifiConfiguration> networks = this.mWifiManager.getConfiguredNetworks();
        int length = networks.size();
        for (int i = 0; i < length; i++) {
            if (networks.get(i).networkId == info.getNetworkId()) {
                return networks.get(i).SSID;
            }
        }
        return null;
    }
}
