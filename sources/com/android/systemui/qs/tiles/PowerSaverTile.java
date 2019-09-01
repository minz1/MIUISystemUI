package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Switch;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class PowerSaverTile extends QSTileImpl<QSTile.BooleanState> {
    private final ContentObserver mBatterySaverObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            PowerSaverTile.this.refreshState();
        }
    };
    private ContentResolver mResolver = this.mContext.getContentResolver();

    public PowerSaverTile(QSHost host) {
        super(host);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mResolver.registerContentObserver(Settings.System.getUriFor("POWER_SAVE_MODE_OPEN"), false, this.mBatterySaverObserver, -1);
        } else {
            this.mResolver.unregisterContentObserver(this.mBatterySaverObserver);
        }
    }

    public Intent getLongClickIntent() {
        return longClickBatterySaverIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mResolver, "POWER_SAVE_MODE_OPEN", 0, -2) != 0) {
            z = false;
        }
        boolean mBatterySaveMode = z;
        Bundle bundle = new Bundle();
        bundle.putBoolean("POWER_SAVE_MODE_OPEN", mBatterySaveMode);
        this.mResolver.call(maybeAddUserId(Uri.parse("content://com.miui.powercenter.powersaver"), ActivityManager.getCurrentUser()), "changePowerMode", null, bundle);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_batterysaver_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean z = false;
        if (Settings.System.getIntForUser(this.mResolver, "POWER_SAVE_MODE_OPEN", 0, -2) != 0) {
            z = true;
        }
        state.value = z;
        state.label = this.mContext.getString(R.string.quick_settings_batterysaver_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_battery_saver_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_battery_saver_off);
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
        return !Constants.IS_TABLET;
    }

    private Intent longClickBatterySaverIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.miui.securitycenter/com.miui.powercenter.savemode.PowerSaveActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }

    private Uri maybeAddUserId(Uri uri, int userId) {
        if (uri == null) {
            return null;
        }
        if (userId == -2 || !"content".equals(uri.getScheme()) || uriHasUserId(uri)) {
            return uri;
        }
        Uri.Builder builder = uri.buildUpon();
        builder.encodedAuthority("" + userId + "@" + uri.getEncodedAuthority());
        return builder.build();
    }

    private boolean uriHasUserId(Uri uri) {
        if (uri == null) {
            return false;
        }
        return !TextUtils.isEmpty(uri.getUserInfo());
    }
}
