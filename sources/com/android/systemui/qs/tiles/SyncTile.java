package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class SyncTile extends QSTileImpl<QSTile.BooleanState> {
    private int mCurrentUserId = 0;
    private Object mStatusChangeListenerHandle;
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            SyncTile.this.refreshState();
        }
    };

    public SyncTile(QSHost host) {
        super(host);
        this.mCurrentUserId = "com.android.systemui".equals(this.mContext.getApplicationInfo().packageName) ? ActivityManager.getCurrentUser() : UserHandle.myUserId();
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
            this.mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(Integer.MAX_VALUE, this.mSyncStatusObserver);
        } else {
            ContentResolver.removeStatusChangeListener(this.mStatusChangeListenerHandle);
        }
    }

    public Intent getLongClickIntent() {
        return longClickVibrateIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        Class<ContentResolver> cls = ContentResolver.class;
        try {
            cls.getMethod("setMasterSyncAutomaticallyAsUser", new Class[]{Boolean.TYPE, Integer.TYPE}).invoke(null, new Object[]{Boolean.valueOf(!isSyncOn()), Integer.valueOf(this.mCurrentUserId)});
        } catch (Exception e) {
            Log.i(this.TAG, "setMasterSyncAutomaticallyAsUser not found.");
            ContentResolver.setMasterSyncAutomatically(true ^ ContentResolver.getMasterSyncAutomatically());
        }
    }

    private boolean isSyncOn() {
        Class<ContentResolver> cls = ContentResolver.class;
        try {
            return ((Boolean) cls.getMethod("getMasterSyncAutomaticallyAsUser", new Class[]{Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(this.mCurrentUserId)})).booleanValue();
        } catch (Exception e) {
            Log.i(this.TAG, "getMasterSyncAutomaticallyAsUser not found.");
            return ContentResolver.getMasterSyncAutomatically();
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_sync_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.value = isSyncOn();
        state.label = this.mContext.getString(R.string.quick_settings_sync_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_sync_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_sync_off);
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

    private Intent longClickVibrateIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.Settings$ManageAccountsSettingsActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }
}
