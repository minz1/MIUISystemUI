package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import miui.util.FeatureParser;

public class PowerModeTile extends QSTileImpl<QSTile.BooleanState> {
    private final ContentObserver mPowerModeObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            PowerModeTile.this.refreshState();
        }
    };
    private ContentResolver mResolver = this.mContext.getContentResolver();

    public PowerModeTile(QSHost host) {
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
            this.mResolver.registerContentObserver(Settings.System.getUriFor("power_mode"), false, this.mPowerModeObserver, -1);
        } else {
            this.mResolver.unregisterContentObserver(this.mPowerModeObserver);
        }
    }

    public Intent getLongClickIntent() {
        return longClickPowerModeIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        String powerMode;
        String powerMode2 = Settings.System.getStringForUser(this.mContext.getContentResolver(), "power_mode", -2);
        if (TextUtils.isEmpty(powerMode2)) {
            powerMode2 = "middle";
        }
        if ("high".equals(powerMode2)) {
            powerMode = "middle";
        } else {
            powerMode = "high";
        }
        SystemProperties.set("persist.sys.aries.power_profile", powerMode);
        Settings.System.putStringForUser(this.mResolver, "power_mode", powerMode, ActivityManager.getCurrentUser());
        this.mContext.sendBroadcastAsUser(new Intent("miui.intent.action.POWER_MODE_CHANGE"), UserHandle.CURRENT);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_powermode_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        String powerMode = Settings.System.getStringForUser(this.mContext.getContentResolver(), "power_mode", -2);
        if (TextUtils.isEmpty(powerMode)) {
            powerMode = "middle";
        }
        state.value = "high".equals(powerMode);
        state.label = this.mContext.getString(R.string.quick_settings_powermode_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_power_high_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_power_high_off);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(state.label);
        sb.append(",");
        sb.append(this.mContext.getString(state.value ? R.string.switch_bar_on : R.string.switch_bar_off));
        state.contentDescription = sb.toString();
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return -1;
    }

    public boolean isAvailable() {
        return FeatureParser.getBoolean("support_power_mode", false);
    }

    private Intent longClickPowerModeIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.Settings$BatterySettingsActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }
}
