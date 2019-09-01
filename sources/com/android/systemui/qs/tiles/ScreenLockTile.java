package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class ScreenLockTile extends QSTileImpl<QSTile.BooleanState> {
    public ScreenLockTile(QSHost host) {
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
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
    }

    public Intent getLongClickIntent() {
        return null;
    }

    public boolean isAvailable() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        ((PowerManager) this.mContext.getSystemService("power")).goToSleep(SystemClock.uptimeMillis());
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_screenlock_label);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.value = false;
        state.state = 1;
        state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_screenlock);
        state.label = this.mHost.getContext().getString(R.string.quick_settings_screenlock_label);
        state.contentDescription = this.mContext.getString(R.string.quick_settings_screenlock_label);
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return -1;
    }
}
