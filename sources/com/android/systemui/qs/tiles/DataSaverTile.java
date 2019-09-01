package com.android.systemui.qs.tiles;

import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.miui.systemui.annotation.Inject;

public class DataSaverTile extends QSTileImpl<QSTile.BooleanState> implements DataSaverController.Listener {
    @Inject
    private DataSaverController mDataSaverController;

    public DataSaverTile(QSHost host) {
        super(host);
        Dependency.inject(this);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mDataSaverController.addCallback(this);
        } else {
            this.mDataSaverController.removeCallback(this);
        }
    }

    public Intent getLongClickIntent() {
        return CellularTile.longClickDataIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (((QSTile.BooleanState) this.mState).value || Prefs.getBoolean(this.mContext, "QsDataSaverDialogShown", false)) {
            toggleDataSaver();
            return;
        }
        SystemUIDialog dialog = new SystemUIDialog(this.mContext);
        dialog.setTitle(R.string.data_saver_enable_title);
        dialog.setMessage(R.string.data_saver_description);
        dialog.setPositiveButton(R.string.data_saver_enable_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DataSaverTile.this.toggleDataSaver();
            }
        });
        dialog.setNegativeButton(17039360, null);
        dialog.setShowForAllUsers(true);
        dialog.show();
        Prefs.putBoolean(this.mContext, "QsDataSaverDialogShown", true);
    }

    /* access modifiers changed from: private */
    public void toggleDataSaver() {
        ((QSTile.BooleanState) this.mState).value = !this.mDataSaverController.isDataSaverEnabled();
        this.mDataSaverController.setDataSaverEnabled(((QSTile.BooleanState) this.mState).value);
        refreshState(Boolean.valueOf(((QSTile.BooleanState) this.mState).value));
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.data_saver);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean z;
        int i;
        if (arg instanceof Boolean) {
            z = ((Boolean) arg).booleanValue();
        } else {
            z = this.mDataSaverController.isDataSaverEnabled();
        }
        state.value = z;
        state.state = state.value ? 2 : 1;
        state.label = this.mContext.getString(R.string.data_saver);
        state.contentDescription = state.label;
        if (state.value) {
            i = R.drawable.ic_data_saver;
        } else {
            i = R.drawable.ic_data_saver_off;
        }
        state.icon = QSTileImpl.ResourceIcon.get(i);
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 284;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_data_saver_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_data_saver_changed_off);
    }

    public void onDataSaverChanged(boolean isDataSaving) {
        refreshState(Boolean.valueOf(isDataSaving));
    }
}
