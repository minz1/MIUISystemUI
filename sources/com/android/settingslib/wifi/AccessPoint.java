package com.android.settingslib.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkKey;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.ScoredNetwork;
import android.net.wifi.IWifiManager;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.R;
import com.android.settingslib.utils.ThreadUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AccessPoint implements Comparable<AccessPoint> {
    static final AtomicInteger sLastId = new AtomicInteger(0);
    private String bssid;
    AccessPointListener mAccessPointListener;
    private int mCarrierApEapType = -1;
    private String mCarrierName = null;
    private WifiConfiguration mConfig;
    private final Context mContext;
    private String mFqdn;
    int mId;
    private WifiInfo mInfo;
    private boolean mIsCarrierAp = false;
    private boolean mIsScoredNetworkMetered = false;
    private String mKey;
    private NetworkInfo mNetworkInfo;
    private String mProviderFriendlyName;
    private int mRssi = Integer.MIN_VALUE;
    public ScanResult mScanResult;
    private final ArraySet<ScanResult> mScanResults = new ArraySet<>();
    private final Object mScanResultsLock = new Object();
    private final Map<String, TimestampedScoredNetwork> mScoredNetworkCache = new HashMap();
    private int mSpeed = 0;
    private Object mTag;
    public int networkId = -1;
    private int pskType = 0;
    private int security;
    private String ssid;
    private int wapiPskType;

    public interface AccessPointListener {
        void onAccessPointChanged(AccessPoint accessPoint);

        void onLevelChanged(AccessPoint accessPoint);
    }

    public AccessPoint(Context context, WifiConfiguration config) {
        this.mContext = context;
        loadConfig(config);
        this.mId = sLastId.incrementAndGet();
    }

    public AccessPoint(Context context, Collection<ScanResult> results) {
        this.mContext = context;
        if (!results.isEmpty()) {
            this.mScanResults.addAll(results);
            ScanResult firstResult = results.iterator().next();
            this.mScanResult = firstResult;
            this.ssid = firstResult.SSID;
            this.bssid = firstResult.BSSID;
            this.security = getSecurity(firstResult);
            if (this.security == 2) {
                this.pskType = getPskType(firstResult);
            }
            updateKey();
            updateRssi();
            this.mIsCarrierAp = firstResult.isCarrierAp;
            this.mCarrierApEapType = firstResult.carrierApEapType;
            this.mCarrierName = firstResult.carrierName;
            this.mId = sLastId.incrementAndGet();
            return;
        }
        throw new IllegalArgumentException("Cannot construct with an empty ScanResult list");
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void loadConfig(WifiConfiguration config) {
        this.ssid = config.SSID == null ? "" : removeDoubleQuotes(config.SSID);
        this.bssid = config.BSSID;
        this.security = getSecurity(config);
        updateKey();
        this.networkId = config.networkId;
        this.mConfig = config;
        this.wapiPskType = config.wapiPskType;
    }

    private void updateKey() {
        StringBuilder builder = new StringBuilder();
        if (TextUtils.isEmpty(getSsidStr())) {
            builder.append(getBssid());
        } else {
            builder.append(getSsidStr());
        }
        builder.append(',');
        builder.append(getSecurity());
        this.mKey = builder.toString();
    }

    public int compareTo(AccessPoint other) {
        if (isActive() && !other.isActive()) {
            return -1;
        }
        if (!isActive() && other.isActive()) {
            return 1;
        }
        if (isReachable() && !other.isReachable()) {
            return -1;
        }
        if (!isReachable() && other.isReachable()) {
            return 1;
        }
        if (isSaved() && !other.isSaved()) {
            return -1;
        }
        if (!isSaved() && other.isSaved()) {
            return 1;
        }
        if (getSpeed() != other.getSpeed()) {
            return other.getSpeed() - getSpeed();
        }
        int difference = MiuiWifiManager.calculateSignalLevel(other.mRssi, 5) - MiuiWifiManager.calculateSignalLevel(this.mRssi, 5);
        if (difference != 0) {
            return difference;
        }
        int difference2 = getSsidStr().compareToIgnoreCase(other.getSsidStr());
        if (difference2 != 0) {
            return difference2;
        }
        return getSsidStr().compareTo(other.getSsidStr());
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof AccessPoint)) {
            return false;
        }
        if (compareTo((AccessPoint) other) == 0) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int result = 0;
        if (this.mInfo != null) {
            result = 0 + (13 * this.mInfo.hashCode());
        }
        return result + (19 * this.mRssi) + (23 * this.networkId) + (29 * this.ssid.hashCode());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AccessPoint(");
        StringBuilder builder = sb.append(this.ssid);
        if (this.bssid != null) {
            builder.append(":");
            builder.append(this.bssid);
        }
        if (isSaved()) {
            builder.append(',');
            builder.append("saved");
        }
        if (isActive()) {
            builder.append(',');
            builder.append("active");
        }
        if (isEphemeral()) {
            builder.append(',');
            builder.append("ephemeral");
        }
        if (isConnectable()) {
            builder.append(',');
            builder.append("connectable");
        }
        if (this.security != 0) {
            builder.append(',');
            builder.append(securityToString(this.security, this.pskType));
        }
        builder.append(",level=");
        builder.append(getLevel());
        if (this.mSpeed != 0) {
            builder.append(",speed=");
            builder.append(this.mSpeed);
        }
        builder.append(",metered=");
        builder.append(isMetered());
        if (isVerboseLoggingEnabled()) {
            builder.append(",rssi=");
            builder.append(this.mRssi);
            builder.append(",scan cache size=");
            builder.append(this.mScanResults.size());
        }
        builder.append(')');
        return builder.toString();
    }

    /* access modifiers changed from: package-private */
    public boolean update(WifiNetworkScoreCache scoreCache, boolean scoringUiEnabled, long maxScoreCacheAgeMillis) {
        boolean scoreChanged = false;
        if (scoringUiEnabled) {
            scoreChanged = updateScores(scoreCache, maxScoreCacheAgeMillis);
        }
        return updateMetered(scoreCache) || scoreChanged;
    }

    private boolean updateScores(WifiNetworkScoreCache scoreCache, long maxScoreCacheAgeMillis) {
        long nowMillis = SystemClock.elapsedRealtime();
        Iterator<ScanResult> it = this.mScanResults.iterator();
        while (it.hasNext()) {
            ScanResult result = it.next();
            ScoredNetwork score = scoreCache.getScoredNetwork(result);
            if (score != null) {
                TimestampedScoredNetwork timedScore = this.mScoredNetworkCache.get(result.BSSID);
                if (timedScore == null) {
                    this.mScoredNetworkCache.put(result.BSSID, new TimestampedScoredNetwork(score, nowMillis));
                } else {
                    timedScore.update(score, nowMillis);
                }
            }
        }
        Iterator<TimestampedScoredNetwork> iterator = this.mScoredNetworkCache.values().iterator();
        iterator.forEachRemaining(new Consumer(nowMillis - maxScoreCacheAgeMillis, iterator) {
            private final /* synthetic */ long f$0;
            private final /* synthetic */ Iterator f$1;

            {
                this.f$0 = r1;
                this.f$1 = r3;
            }

            public final void accept(Object obj) {
                AccessPoint.lambda$updateScores$0(this.f$0, this.f$1, (TimestampedScoredNetwork) obj);
            }
        });
        return updateSpeed();
    }

    static /* synthetic */ void lambda$updateScores$0(long evictionCutoff, Iterator iterator, TimestampedScoredNetwork timestampedScoredNetwork) {
        if (timestampedScoredNetwork.getUpdatedTimestampMillis() < evictionCutoff) {
            iterator.remove();
        }
    }

    private boolean updateSpeed() {
        int oldSpeed = this.mSpeed;
        this.mSpeed = generateAverageSpeedForSsid();
        boolean changed = oldSpeed != this.mSpeed;
        if (isVerboseLoggingEnabled() && changed) {
            Log.i("SettingsLib.AccessPoint", String.format("%s: Set speed to %d", new Object[]{this.ssid, Integer.valueOf(this.mSpeed)}));
        }
        return changed;
    }

    private int generateAverageSpeedForSsid() {
        if (this.mScoredNetworkCache.isEmpty()) {
            return 0;
        }
        if (Log.isLoggable("SettingsLib.AccessPoint", 3)) {
            Log.d("SettingsLib.AccessPoint", String.format("Generating fallbackspeed for %s using cache: %s", new Object[]{getSsidStr(), this.mScoredNetworkCache}));
        }
        int count = 0;
        int totalSpeed = 0;
        for (TimestampedScoredNetwork timedScore : this.mScoredNetworkCache.values()) {
            int speed = timedScore.getScore().calculateBadge(this.mRssi);
            if (speed != 0) {
                count++;
                totalSpeed += speed;
            }
        }
        int speed2 = count == 0 ? 0 : totalSpeed / count;
        if (isVerboseLoggingEnabled()) {
            Log.i("SettingsLib.AccessPoint", String.format("%s generated fallback speed is: %d", new Object[]{getSsidStr(), Integer.valueOf(speed2)}));
        }
        return roundToClosestSpeedEnum(speed2);
    }

    private boolean updateMetered(WifiNetworkScoreCache scoreCache) {
        boolean oldMetering = this.mIsScoredNetworkMetered;
        this.mIsScoredNetworkMetered = false;
        if (!isActive() || this.mInfo == null) {
            Iterator<ScanResult> it = this.mScanResults.iterator();
            while (it.hasNext()) {
                ScoredNetwork score = scoreCache.getScoredNetwork(it.next());
                if (score != null) {
                    this.mIsScoredNetworkMetered |= score.meteredHint;
                }
            }
        } else {
            ScoredNetwork score2 = scoreCache.getScoredNetwork(NetworkKey.createFromWifiInfo(this.mInfo));
            if (score2 != null) {
                this.mIsScoredNetworkMetered |= score2.meteredHint;
            }
        }
        if (oldMetering == this.mIsScoredNetworkMetered) {
            return true;
        }
        return false;
    }

    public static String getKey(ScanResult result) {
        StringBuilder builder = new StringBuilder();
        if (TextUtils.isEmpty(result.SSID)) {
            builder.append(result.BSSID);
        } else {
            builder.append(result.SSID);
        }
        builder.append(',');
        builder.append(getSecurity(result));
        return builder.toString();
    }

    public static String getKey(WifiConfiguration config) {
        StringBuilder builder = new StringBuilder();
        if (TextUtils.isEmpty(config.SSID)) {
            builder.append(config.BSSID);
        } else {
            builder.append(removeDoubleQuotes(config.SSID));
        }
        builder.append(',');
        builder.append(getSecurity(config));
        return builder.toString();
    }

    public String getKey() {
        return this.mKey;
    }

    public boolean matches(WifiConfiguration config) {
        boolean z = false;
        if (!config.isPasspoint() || this.mConfig == null || !this.mConfig.isPasspoint()) {
            if (this.ssid.equals(removeDoubleQuotes(config.SSID)) && this.security == getSecurity(config) && (this.mConfig == null || this.mConfig.shared == config.shared)) {
                z = true;
            }
            return z;
        }
        if (this.ssid.equals(removeDoubleQuotes(config.SSID)) && config.FQDN.equals(this.mConfig.FQDN)) {
            z = true;
        }
        return z;
    }

    public WifiConfiguration getConfig() {
        return this.mConfig;
    }

    public WifiInfo getInfo() {
        return this.mInfo;
    }

    public int getLevel() {
        return MiuiWifiManager.calculateSignalLevel(this.mRssi, 5);
    }

    public Set<ScanResult> getScanResults() {
        ArraySet arraySet;
        synchronized (this.mScanResultsLock) {
            arraySet = new ArraySet(this.mScanResults);
        }
        return arraySet;
    }

    public Map<String, TimestampedScoredNetwork> getScoredNetworkCache() {
        return this.mScoredNetworkCache;
    }

    private void updateRssi() {
        if (!isActive()) {
            int rssi = Integer.MIN_VALUE;
            Iterator<ScanResult> it = this.mScanResults.iterator();
            while (it.hasNext()) {
                ScanResult result = it.next();
                if (result.level > rssi) {
                    rssi = result.level;
                }
            }
            if (rssi == Integer.MIN_VALUE || this.mRssi == Integer.MIN_VALUE) {
                this.mRssi = rssi;
            } else {
                this.mRssi = (this.mRssi + rssi) / 2;
            }
        }
    }

    public boolean isMetered() {
        return this.mIsScoredNetworkMetered || WifiConfiguration.isMetered(this.mConfig, this.mInfo);
    }

    public int getSecurity() {
        return this.security;
    }

    public String getSsidStr() {
        return this.ssid;
    }

    public String getBssid() {
        return this.bssid;
    }

    public CharSequence getSsid() {
        SpannableString str = new SpannableString(this.ssid);
        str.setSpan(new TtsSpan.TelephoneBuilder(this.ssid).build(), 0, this.ssid.length(), 18);
        return str;
    }

    public String getConfigName() {
        if (this.mConfig != null && this.mConfig.isPasspoint()) {
            return this.mConfig.providerFriendlyName;
        }
        if (this.mFqdn != null) {
            return this.mProviderFriendlyName;
        }
        return this.ssid;
    }

    public NetworkInfo.DetailedState getDetailedState() {
        if (this.mNetworkInfo != null) {
            return this.mNetworkInfo.getDetailedState();
        }
        Log.w("SettingsLib.AccessPoint", "NetworkInfo is null, cannot return detailed state");
        return null;
    }

    public String getSummary() {
        return getSettingsSummary(this.mConfig);
    }

    private String getSettingsSummary(WifiConfiguration config) {
        int messageID;
        StringBuilder summary = new StringBuilder();
        if (isActive() && config != null && config.isPasspoint()) {
            summary.append(getSummary(this.mContext, getDetailedState(), false, config.providerFriendlyName));
        } else if (isActive() && config != null && getDetailedState() == NetworkInfo.DetailedState.CONNECTED && this.mIsCarrierAp) {
            summary.append(String.format(this.mContext.getString(R.string.connected_via_carrier), new Object[]{this.mCarrierName}));
        } else if (isActive()) {
            summary.append(getSummary(this.mContext, getDetailedState(), this.mInfo != null && this.mInfo.isEphemeral()));
        } else if (config != null && config.isPasspoint() && config.getNetworkSelectionStatus().isNetworkEnabled()) {
            summary.append(String.format(this.mContext.getString(R.string.available_via_passpoint), new Object[]{config.providerFriendlyName}));
        } else if (config != null && (config.hasNoInternetAccess() || ((config.getNetworkSelectionStatus().getHasEverConnected() && config.isNoInternetAccessExpected()) || (config.getNetworkSelectionStatus().isNetworkPermanentlyDisabled() && config.getNetworkSelectionStatus().getNetworkSelectionDisableReason() == 10)))) {
            if (config.getNetworkSelectionStatus().isNetworkPermanentlyDisabled() || config.isNoInternetAccessExpected()) {
                messageID = R.string.wifi_no_internet_no_reconnect;
            } else {
                messageID = R.string.wifi_no_internet;
            }
            summary.append(this.mContext.getString(messageID));
        } else if (config != null && !config.getNetworkSelectionStatus().isNetworkEnabled()) {
            int networkSelectionDisableReason = config.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
            if (networkSelectionDisableReason != 13) {
                switch (networkSelectionDisableReason) {
                    case 2:
                        summary.append(this.mContext.getString(R.string.wifi_disabled_generic));
                        break;
                    case 3:
                        summary.append(this.mContext.getString(R.string.wifi_disabled_password_failure));
                        break;
                    case 4:
                    case 5:
                        summary.append(this.mContext.getString(R.string.wifi_disabled_network_failure));
                        break;
                }
            } else {
                summary.append(this.mContext.getString(R.string.wifi_check_password_try_again));
            }
        } else if (config != null && config.getNetworkSelectionStatus().isNotRecommended()) {
            summary.append(this.mContext.getString(R.string.wifi_disabled_by_recommendation_provider));
        } else if (this.mIsCarrierAp) {
            summary.append(String.format(this.mContext.getString(R.string.available_via_carrier), new Object[]{this.mCarrierName}));
        } else if (!isReachable()) {
            summary.append(this.mContext.getString(R.string.wifi_not_in_range));
        } else if (config != null) {
            if (config.recentFailure.getAssociationStatus() != 17) {
                summary.append(this.mContext.getString(R.string.wifi_remembered));
            } else {
                summary.append(this.mContext.getString(R.string.wifi_ap_unable_to_handle_new_sta));
            }
        }
        if (isVerboseLoggingEnabled()) {
            summary.append(WifiUtils.buildLoggingSummary(this, config));
        }
        if (config != null && (WifiUtils.isMeteredOverridden(config) || config.meteredHint)) {
            return this.mContext.getResources().getString(R.string.preference_summary_default_combination, new Object[]{WifiUtils.getMeteredLabel(this.mContext, config), summary.toString()});
        } else if (getSpeedLabel() != null && summary.length() != 0) {
            return this.mContext.getResources().getString(R.string.preference_summary_default_combination, new Object[]{getSpeedLabel(), summary.toString()});
        } else if (getSpeedLabel() != null) {
            return getSpeedLabel();
        } else {
            return summary.toString();
        }
    }

    public boolean isActive() {
        return (this.mNetworkInfo == null || (this.networkId == -1 && this.mNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED)) ? false : true;
    }

    public boolean isConnectable() {
        return getLevel() != -1 && getDetailedState() == null;
    }

    public boolean isEphemeral() {
        return (this.mInfo == null || !this.mInfo.isEphemeral() || this.mNetworkInfo == null || this.mNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) ? false : true;
    }

    public boolean isPasspoint() {
        return this.mConfig != null && this.mConfig.isPasspoint();
    }

    private boolean isInfoForThisAccessPoint(WifiConfiguration config, WifiInfo info) {
        if (!isPasspoint() && this.networkId != -1) {
            return this.networkId == info.getNetworkId();
        } else if (config != null) {
            return matches(config);
        } else {
            return this.ssid.equals(removeDoubleQuotes(info.getSSID()));
        }
    }

    public boolean isSaved() {
        return this.networkId != -1;
    }

    public void setTag(Object tag) {
        this.mTag = tag;
    }

    public void generateOpenNetworkConfig() {
        if (this.security != 0) {
            throw new IllegalStateException();
        } else if (this.mConfig == null) {
            this.mConfig = new WifiConfiguration();
            this.mConfig.SSID = convertToQuotedString(this.ssid);
            this.mConfig.allowedKeyManagement.set(0);
        }
    }

    /* access modifiers changed from: package-private */
    public void setScanResults(Collection<ScanResult> scanResults) {
        String key = getKey();
        for (ScanResult result : scanResults) {
            String scanResultKey = getKey(result);
            if (!this.mKey.equals(scanResultKey)) {
                throw new IllegalArgumentException(String.format("ScanResult %s\nkey of %s did not match current AP key %s", new Object[]{result, scanResultKey, key}));
            }
        }
        int oldLevel = getLevel();
        synchronized (this.mScanResultsLock) {
            this.mScanResults.clear();
            this.mScanResults.addAll(scanResults);
        }
        updateRssi();
        int newLevel = getLevel();
        if (newLevel > 0 && newLevel != oldLevel) {
            updateSpeed();
            ThreadUtils.postOnMainThread(new Runnable() {
                public final void run() {
                    AccessPoint.lambda$setScanResults$1(AccessPoint.this);
                }
            });
        }
        ThreadUtils.postOnMainThread(new Runnable() {
            public final void run() {
                AccessPoint.lambda$setScanResults$2(AccessPoint.this);
            }
        });
        if (!scanResults.isEmpty()) {
            ScanResult result2 = scanResults.iterator().next();
            if (this.security == 2) {
                this.pskType = getPskType(result2);
            }
            this.mIsCarrierAp = result2.isCarrierAp;
            this.mCarrierApEapType = result2.carrierApEapType;
            this.mCarrierName = result2.carrierName;
        }
    }

    public static /* synthetic */ void lambda$setScanResults$1(AccessPoint accessPoint) {
        if (accessPoint.mAccessPointListener != null) {
            accessPoint.mAccessPointListener.onLevelChanged(accessPoint);
        }
    }

    public static /* synthetic */ void lambda$setScanResults$2(AccessPoint accessPoint) {
        if (accessPoint.mAccessPointListener != null) {
            accessPoint.mAccessPointListener.onAccessPointChanged(accessPoint);
        }
    }

    public boolean update(WifiConfiguration config, WifiInfo info, NetworkInfo networkInfo) {
        boolean updated = false;
        int oldLevel = getLevel();
        if (info != null && isInfoForThisAccessPoint(config, info)) {
            updated = this.mInfo == null;
            if (this.mConfig != config) {
                update(config);
            }
            if (this.mRssi != info.getRssi() && info.getRssi() != -127) {
                this.mRssi = info.getRssi();
                this.bssid = info.getBSSID();
                if (this.mRssi <= -100 && this.mScanResult != null) {
                    this.mRssi = this.mScanResult.level;
                }
                updated = true;
            } else if (!(this.mNetworkInfo == null || networkInfo == null || this.mNetworkInfo.getDetailedState() == networkInfo.getDetailedState())) {
                updated = true;
            }
            this.mInfo = info;
            this.mNetworkInfo = networkInfo;
        } else if (this.mInfo != null) {
            updated = true;
            this.mInfo = null;
            this.mNetworkInfo = null;
        }
        if (updated && this.mAccessPointListener != null) {
            ThreadUtils.postOnMainThread(new Runnable() {
                public final void run() {
                    AccessPoint.lambda$update$3(AccessPoint.this);
                }
            });
            if (oldLevel != getLevel()) {
                ThreadUtils.postOnMainThread(new Runnable() {
                    public final void run() {
                        AccessPoint.lambda$update$4(AccessPoint.this);
                    }
                });
            }
        }
        return updated;
    }

    public static /* synthetic */ void lambda$update$3(AccessPoint accessPoint) {
        if (accessPoint.mAccessPointListener != null) {
            accessPoint.mAccessPointListener.onAccessPointChanged(accessPoint);
        }
    }

    public static /* synthetic */ void lambda$update$4(AccessPoint accessPoint) {
        if (accessPoint.mAccessPointListener != null) {
            accessPoint.mAccessPointListener.onLevelChanged(accessPoint);
        }
    }

    /* access modifiers changed from: package-private */
    public void update(WifiConfiguration config) {
        this.mConfig = config;
        this.networkId = config != null ? config.networkId : -1;
        ThreadUtils.postOnMainThread(new Runnable() {
            public final void run() {
                AccessPoint.lambda$update$5(AccessPoint.this);
            }
        });
    }

    public static /* synthetic */ void lambda$update$5(AccessPoint accessPoint) {
        if (accessPoint.mAccessPointListener != null) {
            accessPoint.mAccessPointListener.onAccessPointChanged(accessPoint);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    /* access modifiers changed from: package-private */
    public int getSpeed() {
        return this.mSpeed;
    }

    /* access modifiers changed from: package-private */
    public String getSpeedLabel() {
        return getSpeedLabel(this.mSpeed);
    }

    private static int roundToClosestSpeedEnum(int speed) {
        if (speed < 5) {
            return 0;
        }
        if (speed < 7) {
            return 5;
        }
        if (speed < 15) {
            return 10;
        }
        if (speed < 25) {
            return 20;
        }
        return 30;
    }

    /* access modifiers changed from: package-private */
    public String getSpeedLabel(int speed) {
        return getSpeedLabel(this.mContext, speed);
    }

    private static String getSpeedLabel(Context context, int speed) {
        if (speed == 5) {
            return context.getString(R.string.speed_label_slow);
        }
        if (speed == 10) {
            return context.getString(R.string.speed_label_okay);
        }
        if (speed == 20) {
            return context.getString(R.string.speed_label_fast);
        }
        if (speed != 30) {
            return null;
        }
        return context.getString(R.string.speed_label_very_fast);
    }

    public static String getSpeedLabel(Context context, ScoredNetwork scoredNetwork, int rssi) {
        return getSpeedLabel(context, roundToClosestSpeedEnum(scoredNetwork.calculateBadge(rssi)));
    }

    public boolean isReachable() {
        return this.mRssi != Integer.MIN_VALUE;
    }

    public static String getSummary(Context context, String ssid2, NetworkInfo.DetailedState state, boolean isEphemeral, String passpointProvider) {
        if (state == NetworkInfo.DetailedState.CONNECTED && ssid2 == null) {
            if (!TextUtils.isEmpty(passpointProvider)) {
                return String.format(context.getString(R.string.connected_via_passpoint), new Object[]{passpointProvider});
            } else if (isEphemeral) {
                NetworkScorerAppData scorer = ((NetworkScoreManager) context.getSystemService(NetworkScoreManager.class)).getActiveScorer();
                if (scorer == null || scorer.getRecommendationServiceLabel() == null) {
                    return context.getString(R.string.connected_via_network_scorer_default);
                }
                return String.format(context.getString(R.string.connected_via_network_scorer), new Object[]{scorer.getRecommendationServiceLabel()});
            }
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (state == NetworkInfo.DetailedState.CONNECTED) {
            NetworkCapabilities nc = null;
            try {
                nc = cm.getNetworkCapabilities(IWifiManager.Stub.asInterface(ServiceManager.getService("wifi")).getCurrentNetwork());
            } catch (RemoteException e) {
            }
            if (nc != null) {
                if (nc.hasCapability(17)) {
                    return context.getString(context.getResources().getIdentifier("network_available_sign_in", "string", "android"));
                }
                if (!nc.hasCapability(16)) {
                    return context.getString(R.string.wifi_connected_no_internet);
                }
            }
        }
        if (state == null) {
            Log.w("SettingsLib.AccessPoint", "state is null, returning empty summary");
            return "";
        }
        String[] formats = context.getResources().getStringArray(ssid2 == null ? R.array.wifi_status : R.array.wifi_status_with_ssid);
        int index = state != null ? state.ordinal() : 0;
        if (index >= formats.length || formats[index].length() == 0) {
            return "";
        }
        return String.format(formats[index], new Object[]{ssid2});
    }

    public static String getSummary(Context context, NetworkInfo.DetailedState state, boolean isEphemeral) {
        return getSummary(context, null, state, isEphemeral, null);
    }

    public static String getSummary(Context context, NetworkInfo.DetailedState state, boolean isEphemeral, String passpointProvider) {
        return getSummary(context, null, state, isEphemeral, passpointProvider);
    }

    public static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    private static int getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return 3;
        }
        if (wpa2) {
            return 2;
        }
        if (wpa) {
            return 1;
        }
        Log.w("SettingsLib.AccessPoint", "Received abnormal flag string: " + result.capabilities);
        return 0;
    }

    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("DPP")) {
            return 4;
        }
        if (result.capabilities.contains("SAE")) {
            return 5;
        }
        if (result.capabilities.contains("OWE")) {
            return 6;
        }
        if (result.capabilities.contains("WEP")) {
            return 1;
        }
        if (result.capabilities.contains("PSK")) {
            return 2;
        }
        if (result.capabilities.contains("EAP")) {
            return 3;
        }
        if (result.capabilities.contains("WAPI-KEY")) {
            return 7;
        }
        if (result.capabilities.contains("WAPI-CERT")) {
            return 8;
        }
        return 0;
    }

    public static int getSecurity(WifiConfiguration config) {
        int i = 1;
        if (config.allowedKeyManagement.get(1)) {
            return 2;
        }
        if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            return 3;
        }
        if (config.allowedKeyManagement.get(10)) {
            return 4;
        }
        if (config.allowedKeyManagement.get(11)) {
            return 5;
        }
        if (config.allowedKeyManagement.get(12)) {
            return 6;
        }
        if (config.allowedKeyManagement.get(190)) {
            return 7;
        }
        if (config.allowedKeyManagement.get(191)) {
            return 8;
        }
        if (config.wepKeys[0] == null) {
            i = 0;
        }
        return i;
    }

    public static String securityToString(int security2, int pskType2) {
        if (security2 == 1) {
            return "WEP";
        }
        if (security2 == 2) {
            if (pskType2 == 1) {
                return "WPA";
            }
            if (pskType2 == 2) {
                return "WPA2";
            }
            if (pskType2 == 3) {
                return "WPA_WPA2";
            }
            return "PSK";
        } else if (security2 == 3) {
            return "EAP";
        } else {
            if (security2 == 4) {
                return "DPP";
            }
            if (security2 == 5) {
                return "SAE";
            }
            if (security2 == 6) {
                return "OWE";
            }
            return "NONE";
        }
    }

    public static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private static boolean isVerboseLoggingEnabled() {
        return WifiTracker.sVerboseLogging || Log.isLoggable("SettingsLib.AccessPoint", 2);
    }
}
