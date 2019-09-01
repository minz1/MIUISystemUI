package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.List;
import miui.securityspace.CrossUserUtils;
import miui.telephony.SubscriptionInfo;

public class CellularTile extends QSTileImpl<QSTile.BooleanState> {
    private final ActivityStarter mActivityStarter = ((ActivityStarter) Dependency.get(ActivityStarter.class));
    private final NetworkController mController = ((NetworkController) Dependency.get(NetworkController.class));
    /* access modifiers changed from: private */
    public final DataUsageController mDataController = this.mController.getMobileDataController();
    /* access modifiers changed from: private */
    public final CellularDetailAdapter mDetailAdapter = new CellularDetailAdapter();
    /* access modifiers changed from: private */
    public final CellSignalCallback mSignalCallback = new CellSignalCallback() {
        public void setSubs(List<SubscriptionInfo> list) {
        }

        public void setEthernetIndicators(NetworkController.IconState icon) {
        }

        public void setIsImsRegisted(int slot, boolean imsRegisted) {
        }

        public void setVowifi(int slot, boolean vowifi) {
        }

        public void setVolteNoService(int slot, boolean show) {
        }

        public void setSpeechHd(int slot, boolean hd) {
        }

        public void setNetworkNameVoice(int slot, String networkNameVoice) {
        }

        public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
        }
    };

    private static final class CallbackInfo {
        boolean activityIn;
        boolean activityOut;
        boolean airplaneModeEnabled;
        String dataContentDescription;
        int dataTypeIconId;
        boolean enabled;
        String enabledDesc;
        boolean isDataTypeIconWide;
        int mobileSignalIconId;
        boolean noSim;
        boolean roaming;
        String signalContentDescription;
        boolean wifiEnabled;

        private CallbackInfo() {
        }
    }

    private class CellSignalCallback implements NetworkController.SignalCallback {
        /* access modifiers changed from: private */
        public final CallbackInfo mInfo;

        private CellSignalCallback() {
            this.mInfo = new CallbackInfo();
        }

        public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description, boolean isTransient) {
            this.mInfo.wifiEnabled = enabled;
        }

        public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int qsType, boolean activityIn, boolean activityOut, int dataActivityId, int stackedDataIcon, int stackedVoiceIcon, String typeContentDescription, String description, boolean isWide, int subId, boolean roaming) {
            NetworkController.IconState iconState = qsIcon;
            int i = qsType;
            if (iconState != null) {
                this.mInfo.enabled = iconState.visible;
                this.mInfo.mobileSignalIconId = iconState.icon;
                this.mInfo.signalContentDescription = iconState.contentDescription;
                this.mInfo.dataTypeIconId = i;
                this.mInfo.dataContentDescription = typeContentDescription;
                this.mInfo.activityIn = activityIn;
                this.mInfo.activityOut = activityOut;
                this.mInfo.enabledDesc = description;
                this.mInfo.isDataTypeIconWide = i != 0 && isWide;
                this.mInfo.roaming = roaming;
                CellularTile.this.refreshState(this.mInfo);
            }
        }

        public void setNoSims(boolean show) {
            this.mInfo.noSim = show;
            if (this.mInfo.noSim) {
                this.mInfo.mobileSignalIconId = 0;
                this.mInfo.dataTypeIconId = 0;
                this.mInfo.enabled = true;
                this.mInfo.enabledDesc = CellularTile.this.mContext.getString(R.string.keyguard_missing_sim_message_short);
                this.mInfo.signalContentDescription = this.mInfo.enabledDesc;
            }
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setIsAirplaneMode(NetworkController.IconState icon) {
            this.mInfo.airplaneModeEnabled = icon.visible;
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.mDetailAdapter.setMobileDataEnabled(enabled);
            CellularTile.this.refreshState(this.mInfo);
        }

        public void setSubs(List<SubscriptionInfo> list) {
        }

        public void setEthernetIndicators(NetworkController.IconState icon) {
        }

        public void setIsImsRegisted(int slot, boolean imsRegisted) {
        }

        public void setVowifi(int slot, boolean vowifi) {
        }

        public void setVolteNoService(int slot, boolean show) {
        }

        public void setSpeechHd(int slot, boolean hd) {
        }

        public void setNetworkNameVoice(int slot, String networkNameVoice) {
        }

        public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
        }
    }

    private final class CellularDetailAdapter implements DetailAdapter {
        private CellularDetailAdapter() {
        }

        public CharSequence getTitle() {
            return CellularTile.this.mContext.getString(R.string.quick_settings_cellular_detail_title);
        }

        public Boolean getToggleState() {
            if (CellularTile.this.mDataController.isMobileDataSupported()) {
                return Boolean.valueOf(CellularTile.this.mDataController.isMobileDataEnabled());
            }
            return null;
        }

        public boolean getToggleEnabled() {
            return true;
        }

        public Intent getSettingsIntent() {
            return CellularTile.longClickDataIntent();
        }

        public void setToggleState(boolean state) {
            MetricsLogger.action(CellularTile.this.mContext, 155, state);
            CellularTile.this.mDataController.setMobileDataEnabled(state);
        }

        public int getMetricsCategory() {
            return 117;
        }

        public boolean hasHeader() {
            return true;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            int i = 0;
            DataUsageDetailView v = (DataUsageDetailView) (convertView != null ? convertView : LayoutInflater.from(CellularTile.this.mContext).inflate(R.layout.data_usage, parent, false));
            DataUsageController.DataUsageInfo info = CellularTile.this.mDataController.getDataUsageInfo();
            if (info == null) {
                return v;
            }
            v.bind(info);
            View findViewById = v.findViewById(R.id.roaming_text);
            if (!CellularTile.this.mSignalCallback.mInfo.roaming) {
                i = 4;
            }
            findViewById.setVisibility(i);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            CellularTile.this.fireToggleStateChanged(enabled);
        }
    }

    public CellularTile(QSHost host) {
        super(host);
    }

    public QSTile.SignalState newTileState() {
        return new QSTile.SignalState();
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mController.addCallback(this.mSignalCallback);
        } else {
            this.mController.removeCallback(this.mSignalCallback);
        }
    }

    public Intent getLongClickIntent() {
        return longClickDataIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (this.mDataController.isMobileDataSupported()) {
            String str = this.TAG;
            Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + (!((QSTile.BooleanState) this.mState).value));
            this.mDataController.setMobileDataEnabled(((QSTile.BooleanState) this.mState).value ^ true);
        }
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_cellular_detail_title);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        String signalContentDesc;
        int i;
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = this.mSignalCallback.mInfo;
        }
        Resources res = this.mContext.getResources();
        state.label = res.getString(R.string.mobile_data);
        if (!this.mDataController.isMobileDataSupported() || cb.airplaneModeEnabled) {
            if (((QSTile.BooleanState) this.mState).state != 0) {
                String str = this.TAG;
                Log.d(str, "handleUpdateState: airplaneModeEnabled: " + cb.airplaneModeEnabled + ", isMobileDataSupported: " + this.mDataController.isMobileDataSupported());
            }
            state.value = false;
            state.state = 0;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_data_disabled);
        } else {
            state.value = this.mDataController.isMobileDataEnabled();
            state.state = state.value ? 2 : 1;
            if (state.value) {
                i = R.drawable.ic_qs_data_on;
            } else {
                i = R.drawable.ic_qs_data_off;
            }
            state.icon = QSTileImpl.ResourceIcon.get(i);
        }
        if (!cb.enabled || cb.mobileSignalIconId <= 0) {
            signalContentDesc = res.getString(R.string.accessibility_no_signal);
        } else {
            signalContentDesc = cb.signalContentDescription;
        }
        boolean z = cb.noSim;
        int i2 = R.string.switch_bar_off;
        if (z) {
            StringBuilder sb = new StringBuilder();
            sb.append(state.label);
            sb.append(",");
            Context context = this.mContext;
            if (state.value) {
                i2 = R.string.switch_bar_on;
            }
            sb.append(context.getString(i2));
            state.contentDescription = sb.toString();
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(state.label);
            sb2.append(",");
            Context context2 = this.mContext;
            if (state.value) {
                i2 = R.string.switch_bar_on;
            }
            sb2.append(context2.getString(i2));
            sb2.append(",");
            sb2.append(signalContentDesc);
            state.contentDescription = sb2.toString();
        }
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 115;
    }

    public boolean isAvailable() {
        return this.mController.hasMobileDataFeature();
    }

    static Intent longClickDataIntent() {
        if (CrossUserUtils.getCurrentUserId() != 0) {
            return null;
        }
        ComponentName component = ComponentName.unflattenFromString("com.android.phone/.settings.MobileNetworkSettings");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.putExtra(":miui:starting_window_label", "");
        intent.setFlags(335544320);
        return intent;
    }
}
