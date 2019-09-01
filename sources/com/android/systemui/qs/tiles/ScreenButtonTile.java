package com.android.systemui.qs.tiles;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Switch;
import com.android.systemui.Util;
import com.android.systemui.plugins.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class ScreenButtonTile extends QSTileImpl<QSTile.BooleanState> {
    private final ContentObserver mScreenButtonStateObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            ScreenButtonTile.this.refreshState();
        }
    };

    protected class ClickRunnable implements Runnable {
        private boolean disabled;
        private int value;

        ClickRunnable(int value2, boolean disabled2) {
            this.value = value2;
            this.disabled = disabled2;
        }

        public void run() {
            int i;
            if (this.value == 0) {
                AlertDialog dialog = new AlertDialog.Builder(ScreenButtonTile.this.mContext, R.style.Theme_Dialog_Alert).setMessage(285802685).setPositiveButton(17039370, null).create();
                dialog.getWindow().setType(2010);
                dialog.getWindow().addPrivateFlags(16);
                dialog.show();
                return;
            }
            Context access$100 = ScreenButtonTile.this.mContext;
            if (this.disabled) {
                i = com.android.systemui.R.string.auto_disable_screenbuttons_disable_toast_text;
            } else {
                i = com.android.systemui.R.string.auto_disable_screenbuttons_enable_toast_text;
            }
            Util.showSystemOverlayToast(access$100, i, 0);
        }
    }

    public ScreenButtonTile(QSHost host) {
        super(host);
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public Intent getLongClickIntent() {
        return null;
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("screen_buttons_state"), false, this.mScreenButtonStateObserver, -1);
        } else {
            this.mContext.getContentResolver().unregisterContentObserver(this.mScreenButtonStateObserver);
        }
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean z = false;
        boolean screenButtonDisabled = Settings.Secure.getIntForUser(resolver, "screen_buttons_state", 0, -2) != 0;
        int value = Settings.Secure.getIntForUser(resolver, "screen_buttons_has_been_disabled", 0, -2);
        if (value == 0) {
            Settings.Secure.putIntForUser(resolver, "screen_buttons_has_been_disabled", 1, -2);
        }
        Settings.Secure.putIntForUser(resolver, "screen_buttons_state", !screenButtonDisabled ? 1 : 0, -2);
        Handler handler = this.mUiHandler;
        if (!screenButtonDisabled) {
            z = true;
        }
        handler.post(new ClickRunnable(value, z));
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(com.android.systemui.R.string.quick_settings_screenbutton_label);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_buttons_state", 0, -2) != 0) {
            z = true;
        }
        state.value = z;
        state.label = this.mContext.getString(com.android.systemui.R.string.quick_settings_screenbutton_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(com.android.systemui.R.drawable.ic_qs_screen_button_enabled);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(com.android.systemui.R.drawable.ic_qs_screen_button_disabled);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(state.label);
        sb.append(",");
        sb.append(this.mContext.getString(state.value ? com.android.systemui.R.string.switch_bar_on : com.android.systemui.R.string.switch_bar_off));
        state.contentDescription = sb.toString();
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return -1;
    }
}
