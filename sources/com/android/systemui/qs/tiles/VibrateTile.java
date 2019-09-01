package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Vibrator;
import android.provider.Settings;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.SilentModeObserverController;
import miui.util.AudioManagerHelper;

public class VibrateTile extends QSTileImpl<QSTile.BooleanState> implements SilentModeObserverController.SilentModeListener {
    private int mCurrentUserId;
    private final ContentResolver mResolver;
    private final SilentModeObserverController mSilentModeObserverController;
    private ContentObserver mVibrateEnableObserver;

    public VibrateTile(QSHost host) {
        super(host);
        this.mCurrentUserId = 0;
        this.mVibrateEnableObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                VibrateTile.this.refreshState();
            }
        };
        this.mResolver = this.mContext.getContentResolver();
        this.mCurrentUserId = -1;
        this.mSilentModeObserverController = (SilentModeObserverController) Dependency.get(SilentModeObserverController.class);
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
            this.mResolver.registerContentObserver(Settings.System.getUriFor("vibrate_in_silent"), false, this.mVibrateEnableObserver, this.mCurrentUserId);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("vibrate_in_normal"), false, this.mVibrateEnableObserver, this.mCurrentUserId);
            this.mSilentModeObserverController.addCallback(this);
            return;
        }
        this.mResolver.unregisterContentObserver(this.mVibrateEnableObserver);
        this.mSilentModeObserverController.removeCallback(this);
    }

    public Intent getLongClickIntent() {
        return longClickVibrateIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        AudioManagerHelper.toggleVibrateSetting(this.mContext);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_vibrate_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.value = AudioManagerHelper.isVibrateEnabled(this.mContext);
        state.label = this.mContext.getString(R.string.quick_settings_vibrate_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_vibrate_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_vibrate_off);
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
        return ((Vibrator) this.mContext.getSystemService("vibrator")).hasVibrator();
    }

    private Intent longClickVibrateIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.Settings$SoundSettingsActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }

    public void onSilentModeChanged(boolean enabled) {
        refreshState();
    }
}
