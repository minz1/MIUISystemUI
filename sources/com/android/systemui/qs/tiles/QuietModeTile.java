package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.provider.MiuiSettings;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.volume.VolumeUtil;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.SilentModeObserverController;

public class QuietModeTile extends QSTileImpl<QSTile.BooleanState> implements SilentModeObserverController.SilentModeListener {
    private final SilentModeObserverController mSilentModeObserverController = ((SilentModeObserverController) Dependency.get(SilentModeObserverController.class));

    public QuietModeTile(QSHost host) {
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
            this.mSilentModeObserverController.addCallback(this);
        } else {
            this.mSilentModeObserverController.removeCallback(this);
        }
    }

    public Intent getLongClickIntent() {
        return longClickQuietModeIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        int i = 1;
        if (MiuiSettings.SilenceMode.isSupported) {
            boolean enabled = MiuiSettings.SilenceMode.getZenMode(this.mContext) != 1;
            Context context = this.mContext;
            if (!enabled) {
                i = 0;
            }
            VolumeUtil.setSilenceMode(context, i, null);
            return;
        }
        MiuiSettings.AntiSpam.setQuietMode(this.mContext, true ^ ((QSTile.BooleanState) this.mState).value);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_quietmode_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.value = MiuiSettings.SilenceMode.isSupported ? MiuiSettings.SilenceMode.getZenMode(this.mContext) == 1 : MiuiSettings.AntiSpam.isQuietModeEnable(this.mContext);
        state.label = this.mContext.getString(R.string.quick_settings_quietmode_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_dnd_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_dnd_off);
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
        return ((ConnectivityManager) this.mContext.getSystemService("connectivity")).isNetworkSupported(0);
    }

    private Intent longClickQuietModeIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.Settings$MiuiSilentModeAcivity");
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
