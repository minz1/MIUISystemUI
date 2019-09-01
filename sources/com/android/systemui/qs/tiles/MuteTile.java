package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.volume.VolumeUtil;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.SilentModeObserverController;
import miui.util.AudioManagerHelper;

public class MuteTile extends QSTileImpl<QSTile.BooleanState> implements SilentModeObserverController.SilentModeListener {
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.media.RINGER_MODE_CHANGED".equals(intent.getAction())) {
                MuteTile.this.refreshState();
            }
        }
    };
    private final SilentModeObserverController mSilentModeObserverController = ((SilentModeObserverController) Dependency.get(SilentModeObserverController.class));

    public MuteTile(QSHost host) {
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
            UserHandle user = UserHandle.ALL;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.RINGER_MODE_CHANGED");
            this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, user, filter, null, null);
            this.mSilentModeObserverController.addCallback(this);
            return;
        }
        this.mSilentModeObserverController.removeCallback(this);
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    public Intent getLongClickIntent() {
        return longClickMuteIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        int i = 4;
        if (MiuiSettings.SilenceMode.isSupported) {
            boolean enabled = MiuiSettings.SilenceMode.getZenMode(this.mContext) != 4;
            refreshState(enabled ? null : ARG_SHOW_TRANSIENT_ENABLING);
            Context context = this.mContext;
            if (!enabled) {
                i = 0;
            }
            VolumeUtil.setSilenceMode(context, i, null);
            return;
        }
        AudioManagerHelper.toggleSilent(this.mContext, 4);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_mute_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean enabled = false;
        boolean transientEnabling = arg == ARG_SHOW_TRANSIENT_ENABLING;
        int zenMode = MiuiSettings.SilenceMode.getZenMode(this.mContext);
        if (!transientEnabling && !MiuiSettings.SilenceMode.isSupported) {
            enabled = AudioManagerHelper.isSilentEnabled(this.mContext);
        } else if (zenMode == 4) {
            enabled = true;
        }
        state.value = enabled;
        state.label = this.mContext.getString(R.string.quick_settings_mute_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_mute_on);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_mute_off);
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

    private Intent longClickMuteIntent() {
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
