package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
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
import miui.securityspace.CrossUserUtils;

public class PowerSaverExtremeTile extends QSTileImpl<QSTile.BooleanState> {
    private final ContentObserver mObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            PowerSaverExtremeTile.this.refreshState();
        }
    };
    private ContentResolver mResolver = this.mContext.getContentResolver();

    public PowerSaverExtremeTile(QSHost host) {
        super(host);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor("EXTREME_POWER_MODE_ENABLE"), false, this.mObserver, -1);
        } else {
            this.mResolver.unregisterContentObserver(this.mObserver);
        }
    }

    public Intent getLongClickIntent() {
        Intent intent = new Intent("miui.intent.action.EXTREME_POWER_ENTRY_ACTIVITY");
        intent.setFlags(335544320);
        return intent;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mResolver, "EXTREME_POWER_MODE_ENABLE", 0, -2) != 0) {
            z = false;
        }
        boolean mBatterySaveMode = z;
        Bundle bundle = new Bundle();
        bundle.putString("SOURCE", "systemui");
        bundle.putBoolean("EXTREME_POWER_SAVE_MODE_OPEN", mBatterySaveMode);
        this.mResolver.call(maybeAddUserId(Uri.parse("content://com.miui.powerkeeper.configure"), ActivityManager.getCurrentUser()), "changeExtremePowerMode", null, bundle);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_extreme_batterysaver_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mResolver, "EXTREME_POWER_MODE_ENABLE", 0, -2) != 0) {
            z = true;
        }
        state.value = z;
        state.label = this.mContext.getString(R.string.quick_settings_extreme_batterysaver_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_extreme_battery_saver_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_extreme_battery_saver_off);
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
        return Constants.SUPPORT_EXTREME_BATTERY_SAVER && !Constants.IS_TABLET && CrossUserUtils.getCurrentUserId() == 0;
    }

    private Uri maybeAddUserId(Uri uri, int userId) {
        if (uri == null) {
            return null;
        }
        if (userId == -2 || !"content".equals(uri.getScheme()) || uriHasUserId(uri)) {
            return uri;
        }
        Uri.Builder builder = uri.buildUpon();
        builder.encodedAuthority(Integer.toString(userId) + "@" + uri.getEncodedAuthority());
        return builder.build();
    }

    private boolean uriHasUserId(Uri uri) {
        if (uri == null) {
            return false;
        }
        return !TextUtils.isEmpty(uri.getUserInfo());
    }
}
