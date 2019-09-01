package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class EditTile extends QSTileImpl<QSTile.BooleanState> {
    public EditTile(QSHost host) {
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
    public void handleClick() {
        showEdit(true);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
    }

    public Intent getLongClickIntent() {
        return null;
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_edit_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_edit_label);
        state.state = 1;
        state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_edit);
        state.contentDescription = state.label;
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return -1;
    }
}
