package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class AirplaneModeTile extends QSTileImpl<QSTile.BooleanState> {
    private boolean mListening;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                AirplaneModeTile.this.refreshState();
            }
        }
    };
    private final GlobalSetting mSetting = new GlobalSetting(this.mContext, this.mHandler, "airplane_mode_on") {
        /* access modifiers changed from: protected */
        public void handleValueChanged(int value) {
            AirplaneModeTile.this.handleRefreshState(Integer.valueOf(value));
        }
    };

    public AirplaneModeTile(QSHost host) {
        super(host);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleClick() {
        String str = this.TAG;
        Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + (!((QSTile.BooleanState) this.mState).value));
        MetricsLogger.action(this.mContext, getMetricsCategory(), ((QSTile.BooleanState) this.mState).value ^ true);
        setEnabled(((QSTile.BooleanState) this.mState).value ^ true);
    }

    private void setEnabled(boolean enabled) {
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).setAirplaneMode(enabled);
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.AIRPLANE_MODE_SETTINGS");
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.airplane_mode);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int i = 1;
        boolean airplaneMode = (arg instanceof Integer ? ((Integer) arg).intValue() : this.mSetting.getValue()) != 0;
        state.value = airplaneMode;
        state.label = this.mContext.getString(R.string.airplane_mode);
        if (airplaneMode) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_airplane_enable);
        } else {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_signal_airplane_disable);
        }
        if (airplaneMode) {
            i = 2;
        }
        state.state = i;
        StringBuilder sb = new StringBuilder();
        sb.append(state.label);
        sb.append(",");
        sb.append(this.mContext.getString(state.value ? R.string.switch_bar_on : R.string.switch_bar_off));
        state.contentDescription = sb.toString();
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 112;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_airplane_changed_off);
    }

    public void handleSetListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.AIRPLANE_MODE");
                this.mContext.registerReceiver(this.mReceiver, filter);
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
            }
            this.mSetting.setListening(listening);
        }
    }
}
