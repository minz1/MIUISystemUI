package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class GpsTile extends QSTileImpl<QSTile.BooleanState> {
    private int mCurrentUserId = ActivityManager.getCurrentUser();
    private final ContentObserver mLocationAllowedObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            GpsTile.this.refreshState();
        }
    };
    private final ContentResolver mResolver = this.mContext.getContentResolver();

    public GpsTile(QSHost host) {
        super(host);
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor("location_providers_allowed"), false, this.mLocationAllowedObserver, -1);
        } else {
            this.mResolver.unregisterContentObserver(this.mLocationAllowedObserver);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    public Intent getLongClickIntent() {
        return longClickGPSIntent();
    }

    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.location.gps");
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        String str = this.TAG;
        Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + (!((QSTile.BooleanState) this.mState).value));
        Settings.Secure.setLocationProviderEnabledForUser(this.mResolver, "gps", ((QSTile.BooleanState) this.mState).value ^ true, this.mCurrentUserId);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(Constants.SUPPORT_DUAL_GPS ? R.string.quick_settings_dual_location_label : R.string.quick_settings_gps_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int i;
        state.value = Settings.Secure.isLocationProviderEnabledForUser(this.mResolver, "gps", this.mCurrentUserId);
        if (state.value) {
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_on);
        } else {
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_location_off);
        }
        state.label = this.mContext.getString(Constants.SUPPORT_DUAL_GPS ? R.string.quick_settings_dual_location_label : R.string.quick_settings_gps_label);
        if (state.value) {
            i = Constants.SUPPORT_DUAL_GPS ? R.drawable.ic_qs_dual_location_enabled : R.drawable.ic_signal_location_enable;
        } else {
            i = Constants.SUPPORT_DUAL_GPS ? R.drawable.ic_qs_dual_location_disabled : R.drawable.ic_signal_location_disable;
        }
        state.icon = QSTileImpl.ResourceIcon.get(i);
        state.state = state.value ? 2 : 1;
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    private Intent longClickGPSIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.Settings$LocationSettingsActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }

    public int getMetricsCategory() {
        return -1;
    }
}
