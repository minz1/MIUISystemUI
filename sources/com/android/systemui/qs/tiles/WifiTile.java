package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.List;
import miui.telephony.SubscriptionInfo;

public class WifiTile extends QSTileImpl<QSTile.BooleanState> {
    /* access modifiers changed from: private */
    public static final Intent WIFI_SETTINGS = new Intent("android.settings.WIFI_SETTINGS");
    /* access modifiers changed from: private */
    public final ActivityStarter mActivityStarter = ((ActivityStarter) Dependency.get(ActivityStarter.class));
    protected final NetworkController mController = ((NetworkController) Dependency.get(NetworkController.class));
    /* access modifiers changed from: private */
    public final WifiDetailAdapter mDetailAdapter = ((WifiDetailAdapter) createDetailAdapter());
    protected final WifiSignalCallback mSignalCallback = new WifiSignalCallback();
    private final QSTile.SignalState mStateBeforeClick = newTileState();
    /* access modifiers changed from: private */
    public boolean mTransientEnabling;
    /* access modifiers changed from: private */
    public final NetworkController.AccessPointController mWifiController = this.mController.getAccessPointController();
    private boolean mWifiEnabled;

    protected static final class CallbackInfo {
        protected final String TAG = "WifiTile";
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        boolean isTransient;
        String ssid;
        String wifiSignalContentDescription;
        int wifiSignalIconId;

        protected CallbackInfo() {
        }

        public boolean isChanged(boolean enabled2, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn2, boolean activityOut2, String description, boolean isTransient2) {
            this.activityIn = activityIn2;
            this.activityOut = activityOut2;
            boolean isChanged = false;
            if (this.enabled != enabled2) {
                Log.d("WifiTile", "isChanged: enabled from: " + this.enabled + ", to: " + enabled2);
                this.enabled = enabled2;
                isChanged = true;
            }
            if (this.connected != qsIcon.visible) {
                Log.d("WifiTile", "isChanged: connected from: " + this.connected + ", to: " + qsIcon.visible);
                this.connected = qsIcon.visible;
                isChanged = true;
            }
            if (this.isTransient != isTransient2) {
                Log.d("WifiTile", "isChanged: isTransient from: " + this.isTransient + ", to: " + isTransient2);
                this.isTransient = isTransient2;
                isChanged = true;
            }
            if (this.wifiSignalIconId != qsIcon.icon) {
                this.wifiSignalIconId = qsIcon.icon;
                isChanged = true;
            }
            if (!TextUtils.equals(this.ssid, description)) {
                this.ssid = description;
                isChanged = true;
            }
            if (TextUtils.equals(this.wifiSignalContentDescription, qsIcon.contentDescription)) {
                return isChanged;
            }
            this.wifiSignalContentDescription = qsIcon.contentDescription;
            return true;
        }

        public String toString() {
            return "CallbackInfo[" + "enabled=" + this.enabled + ",connected=" + this.connected + ",wifiSignalIconId=" + this.wifiSignalIconId + ",ssid=" + this.ssid + ",activityIn=" + this.activityIn + ",activityOut=" + this.activityOut + ",wifiSignalContentDescription=" + this.wifiSignalContentDescription + ",isTransient=" + this.isTransient + ']';
        }
    }

    protected class WifiDetailAdapter implements DetailAdapter, QSDetailItems.Callback, NetworkController.AccessPointController.AccessPointCallback {
        private AccessPoint[] mAccessPoints;
        private QSDetailItems mItems;

        protected WifiDetailAdapter() {
        }

        public CharSequence getTitle() {
            return WifiTile.this.mContext.getString(R.string.quick_settings_wifi_label);
        }

        public Intent getSettingsIntent() {
            return WifiTile.WIFI_SETTINGS;
        }

        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) WifiTile.this.mState).value);
        }

        public boolean getToggleEnabled() {
            return true;
        }

        public void setToggleState(boolean state) {
            if (WifiTile.DEBUG) {
                String access$1300 = WifiTile.this.TAG;
                Log.d(access$1300, "setToggleState " + state);
            }
            MetricsLogger.action(WifiTile.this.mContext, 153, state);
            WifiTile.this.mController.setWifiEnabled(state);
        }

        public int getMetricsCategory() {
            return 152;
        }

        public boolean hasHeader() {
            return true;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (WifiTile.DEBUG) {
                String access$1600 = WifiTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("createDetailView convertView=");
                sb.append(convertView != null);
                Log.d(access$1600, sb.toString());
            }
            this.mAccessPoints = null;
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Wifi");
            this.mItems.setCallback(this);
            WifiTile.this.mWifiController.updateVerboseLoggingLevel();
            WifiTile.this.mWifiController.scanForAccessPoints();
            setItemsVisible(((QSTile.BooleanState) WifiTile.this.mState).value);
            return this.mItems;
        }

        public void onAccessPointsChanged(List<AccessPoint> accessPoints) {
            this.mAccessPoints = (AccessPoint[]) accessPoints.toArray(new AccessPoint[accessPoints.size()]);
            this.mAccessPoints = WifiTileHelper.filterUnreachableAPs(this.mAccessPoints);
            if (WifiTile.this.isShowingDetail()) {
                updateItems();
            }
        }

        public void onSettingsActivityTriggered(Intent settingsIntent) {
            WifiTile.this.mActivityStarter.postStartActivityDismissingKeyguard(settingsIntent, 0);
        }

        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                AccessPoint ap = (AccessPoint) item.tag;
                if (!ap.isActive() && WifiTile.this.mWifiController.connect(ap)) {
                    WifiTile.this.mHost.collapsePanels();
                }
                WifiTile.this.showDetail(false);
            }
        }

        public void onDetailItemDisconnect(QSDetailItems.Item item) {
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems != null) {
                this.mItems.setItemsVisible(visible);
            }
        }

        /* access modifiers changed from: private */
        public void updateItems() {
            int i;
            if (this.mItems != null) {
                if ((this.mAccessPoints == null || this.mAccessPoints.length <= 0) && WifiTile.this.mSignalCallback.mInfo.enabled) {
                    WifiTile.this.fireScanStateChanged(true);
                } else {
                    WifiTile.this.fireScanStateChanged(false);
                }
                if (WifiTile.this.mSignalCallback.mInfo.enabled) {
                    QSDetailItems.Item[] items = null;
                    if (this.mAccessPoints == null || this.mAccessPoints.length <= 0) {
                        this.mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty, R.string.quick_settings_wifi_detail_empty_text);
                    } else {
                        items = new QSDetailItems.Item[this.mAccessPoints.length];
                        for (int i2 = 0; i2 < this.mAccessPoints.length; i2++) {
                            AccessPoint ap = this.mAccessPoints[i2];
                            QSDetailItems.Item item = new QSDetailItems.Item();
                            item.tag = ap;
                            item.icon = WifiTile.this.mWifiController.getIcon(ap);
                            item.line1 = ap.getSsid();
                            item.line2 = ap.isActive() ? ap.getSummary() : null;
                            if (ap.getSecurity() != 0) {
                                i = R.drawable.ic_qs_wifi_lock;
                            } else {
                                i = -1;
                            }
                            item.icon2 = i;
                            items[i2] = item;
                        }
                    }
                    this.mItems.setItems(items);
                } else {
                    this.mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty, R.string.wifi_is_off);
                    this.mItems.setItems(null);
                }
            }
        }
    }

    protected final class WifiSignalCallback implements NetworkController.SignalCallback {
        final CallbackInfo mInfo = new CallbackInfo();

        protected WifiSignalCallback() {
        }

        public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description, boolean isTransient) {
            boolean z;
            if (WifiTile.DEBUG) {
                String access$100 = WifiTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("onWifiSignalChanged enabled=");
                z = enabled;
                sb.append(z);
                Log.d(access$100, sb.toString());
            } else {
                z = enabled;
            }
            if (!this.mInfo.isChanged(z, statusIcon, qsIcon, activityIn, activityOut, description, isTransient)) {
                if (WifiTile.DEBUG) {
                    Log.d(WifiTile.this.TAG, "setWifiIndicators: ignore in/out info change");
                }
                return;
            }
            if (WifiTile.this.isShowingDetail()) {
                WifiTile.this.mDetailAdapter.updateItems();
            }
            if (!WifiTile.this.mTransientEnabling || isTransient) {
                WifiTile.this.refreshState();
            } else {
                Log.d(WifiTile.this.TAG, "setWifiIndicators: ignore when enabling state is not ready");
            }
        }

        public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int qsType, boolean activityIn, boolean activityOut, int dataActivityId, int stackedDataIcon, int stackedVoiceIcon, String typeContentDescription, String description, boolean isWide, int slot, boolean roaming) {
        }

        public void setSubs(List<SubscriptionInfo> list) {
        }

        public void setNoSims(boolean show) {
        }

        public void setEthernetIndicators(NetworkController.IconState icon) {
        }

        public void setIsAirplaneMode(NetworkController.IconState icon) {
        }

        public void setMobileDataEnabled(boolean enabled) {
        }

        public void setIsImsRegisted(int slot, boolean imsRegisted) {
        }

        public void setVolteNoService(int slot, boolean show) {
        }

        public void setSpeechHd(int slot, boolean hd) {
        }

        public void setVowifi(int slot, boolean vowifi) {
        }

        public void setNetworkNameVoice(int slot, String networkNameVoice) {
        }

        public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
        }
    }

    public WifiTile(QSHost host) {
        super(host);
    }

    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mController.addCallback(this.mSignalCallback);
        } else {
            this.mController.removeCallback(this.mSignalCallback);
        }
    }

    public void setDetailListening(boolean listening) {
        if (listening) {
            this.mWifiController.addAccessPointCallback(this.mDetailAdapter);
        } else {
            this.mWifiController.removeAccessPointCallback(this.mDetailAdapter);
        }
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    /* access modifiers changed from: protected */
    public DetailAdapter createDetailAdapter() {
        return new WifiDetailAdapter();
    }

    public Intent getLongClickIntent() {
        return WIFI_SETTINGS;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((QSTile.BooleanState) this.mState).isTransient) {
            Log.d(this.TAG, "handleClick: not ready, ignore");
            return;
        }
        ((QSTile.BooleanState) this.mState).copyTo(this.mStateBeforeClick);
        this.mWifiEnabled = !((QSTile.BooleanState) this.mState).value;
        refreshState(this.mWifiEnabled ? ARG_SHOW_TRANSIENT_ENABLING : null);
        this.mController.setWifiEnabled(this.mWifiEnabled);
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        if (!this.mWifiController.canConfigWifi()) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.settings.WIFI_SETTINGS"), 0);
            return;
        }
        showDetail(true);
        if (!((QSTile.BooleanState) this.mState).value) {
            this.mController.setWifiEnabled(true);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_wifi_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int i;
        if (DEBUG) {
            Log.d(this.TAG, "handleUpdateState arg=" + arg);
        }
        CallbackInfo cb = this.mSignalCallback.mInfo;
        this.mTransientEnabling = arg == ARG_SHOW_TRANSIENT_ENABLING;
        boolean wifiStateEnabling = cb.isTransient && this.mWifiEnabled;
        state.isTransient = this.mTransientEnabling || cb.isTransient;
        boolean wifiConnected = cb.enabled && cb.wifiSignalIconId > 0 && cb.ssid != null;
        boolean wifiNotConnected = cb.wifiSignalIconId > 0 && cb.ssid == null;
        if (state.value != cb.enabled) {
            this.mDetailAdapter.setItemsVisible(cb.enabled);
            fireToggleStateChanged(cb.enabled);
        }
        int i2 = 2;
        state.state = 2;
        state.dualTarget = true;
        state.value = this.mTransientEnabling || wifiStateEnabling || cb.enabled;
        StringBuffer minimalContentDescription = new StringBuffer();
        Resources r = this.mContext.getResources();
        if (state.isTransient) {
            state.label = r.getString(R.string.quick_settings_wifi_label);
        } else if (!state.value) {
            state.label = r.getString(R.string.quick_settings_wifi_label);
        } else if (wifiConnected) {
            state.label = removeDoubleQuotes(cb.ssid);
        } else if (wifiNotConnected) {
            state.label = r.getString(R.string.quick_settings_wifi_label);
        } else {
            state.label = r.getString(R.string.quick_settings_wifi_label);
        }
        minimalContentDescription.append(this.mContext.getString(R.string.quick_settings_wifi_label));
        minimalContentDescription.append(",");
        minimalContentDescription.append(this.mContext.getString(state.value ? R.string.switch_bar_on : R.string.switch_bar_off));
        minimalContentDescription.append(",");
        if (state.value && wifiConnected) {
            minimalContentDescription.append(cb.wifiSignalContentDescription);
            minimalContentDescription.append(",");
            minimalContentDescription.append(removeDoubleQuotes(cb.ssid));
        }
        if (!state.value) {
            i2 = 1;
        }
        state.state = i2;
        if (state.value) {
            i = R.drawable.ic_qs_wifi_on;
        } else {
            i = R.drawable.ic_qs_wifi_off;
        }
        state.icon = QSTileImpl.ResourceIcon.get(i);
        state.contentDescription = minimalContentDescription.toString();
        state.dualLabelContentDescription = r.getString(R.string.accessibility_quick_settings_open_settings, new Object[]{getTileLabel()});
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 126;
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnnouncementBeDelayed() {
        return this.mStateBeforeClick.value == ((QSTile.BooleanState) this.mState).value;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_wifi_changed_off);
    }

    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi");
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
