package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.HotspotController;
import miui.securityspace.CrossUserUtils;

public class HotspotTile extends QSTileImpl<QSTile.AirplaneBooleanState> {
    static final Intent TETHER_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity"));
    private final GlobalSetting mAirplaneMode = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
        /* access modifiers changed from: protected */
        public void handleValueChanged(int value) {
            HotspotTile.this.refreshState();
        }
    };
    private final Callback mCallback = new Callback();
    private final HotspotController mController = ((HotspotController) Dependency.get(HotspotController.class));
    private boolean mListening;

    private final class Callback implements HotspotController.Callback {
        private Callback() {
        }

        public void onHotspotChanged(boolean enabled) {
            HotspotTile.this.refreshState(Boolean.valueOf(enabled));
        }
    }

    public HotspotTile(QSHost host) {
        super(host);
    }

    public boolean isAvailable() {
        return this.mController.isHotspotSupported() && CrossUserUtils.getCurrentUserId() == 0;
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public QSTile.AirplaneBooleanState newTileState() {
        return new QSTile.AirplaneBooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                this.mController.addCallback(this.mCallback);
                refreshState();
            } else {
                this.mController.removeCallback(this.mCallback);
            }
            this.mAirplaneMode.setListening(listening);
        }
    }

    public Intent getLongClickIntent() {
        return new Intent(TETHER_SETTINGS);
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        boolean isEnabled = ((QSTile.AirplaneBooleanState) this.mState).value;
        if ((isEnabled || this.mAirplaneMode.getValue() == 0) && this.mController.isHotspotReady()) {
            String str = this.TAG;
            Log.d(str, "handleClick: from: mState.value: " + ((QSTile.AirplaneBooleanState) this.mState).value + ", to: " + (!((QSTile.AirplaneBooleanState) this.mState).value));
            refreshState(isEnabled ? null : ARG_SHOW_TRANSIENT_ENABLING);
            this.mController.setHotspotEnabled(!isEnabled);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_hotspot_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.AirplaneBooleanState state, Object arg) {
        QSTile.Icon icon;
        int i = 0;
        boolean transientEnabling = arg == ARG_SHOW_TRANSIENT_ENABLING;
        checkIfRestrictionEnforcedByAdminOnly(state, "no_config_tethering");
        state.value = transientEnabling || this.mController.isHotspotEnabled();
        state.label = this.mContext.getString(R.string.quick_settings_hotspot_label);
        if (!state.value) {
            icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_hotspot_disabled);
        } else if (state.isTransient) {
            icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_hotspot_enabled);
        } else {
            icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_hotspot_enabled);
        }
        state.icon = icon;
        boolean wasAirplane = state.isAirplaneMode;
        state.isAirplaneMode = this.mAirplaneMode.getValue() != 0;
        state.isTransient = this.mController.isHotspotTransient();
        if (state.isTransient) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_hotspot_enabled);
        } else if (state.isAirplaneMode) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_hotspot_disabled);
        } else if (wasAirplane) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_hotspot_disabled);
        }
        if (!state.isAirplaneMode) {
            i = (state.value || state.isTransient) ? 2 : 1;
        }
        state.state = i;
        StringBuilder sb = new StringBuilder();
        sb.append(state.label);
        sb.append(",");
        sb.append(this.mContext.getString(state.value ? R.string.switch_bar_on : R.string.switch_bar_off));
        state.contentDescription = sb.toString();
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 120;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.AirplaneBooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_off);
    }
}
