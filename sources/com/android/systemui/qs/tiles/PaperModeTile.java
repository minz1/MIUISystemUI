package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.PaperModeController;

public class PaperModeTile extends QSTileImpl<QSTile.BooleanState> implements PaperModeController.PaperModeListener {
    private final PaperModeController mPaperModeController = ((PaperModeController) Dependency.get(PaperModeController.class));

    public PaperModeTile(QSHost host) {
        super(host);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleClick() {
        String str = this.TAG;
        Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + (!((QSTile.BooleanState) this.mState).value));
        boolean newState = ((QSTile.BooleanState) this.mState).value ^ true;
        refreshState(Boolean.valueOf(newState));
        this.mPaperModeController.setEnabled(newState);
    }

    public Intent getLongClickIntent() {
        return longClickPaperModeIntent();
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_papermode_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_papermode_label);
        boolean isAvailable = this.mPaperModeController.isAvailable();
        int i = R.string.switch_bar_off;
        if (!isAvailable) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_paper_mode_off);
            state.contentDescription = state.label + "," + this.mContext.getString(R.string.switch_bar_off);
            state.state = 0;
            return;
        }
        if (arg instanceof Boolean) {
            boolean value = ((Boolean) arg).booleanValue();
            if (value != state.value) {
                state.value = value;
            } else {
                return;
            }
        } else {
            state.value = this.mPaperModeController.isEnabled();
        }
        if (state.value) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_paper_mode_on);
            state.state = 2;
        } else {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_paper_mode_off);
            state.state = 1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(state.label);
        sb.append(",");
        Context context = this.mContext;
        if (state.value) {
            i = R.string.switch_bar_on;
        }
        sb.append(context.getString(i));
        state.contentDescription = sb.toString();
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return -1;
    }

    public boolean isAvailable() {
        return Constants.SUPPORT_SCREEN_PAPER_MODE;
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mPaperModeController.addCallback(this);
        } else {
            this.mPaperModeController.removeCallback(this);
        }
    }

    private Intent longClickPaperModeIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/.display.ScreenPaperModeActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }

    public void onPaperModeChanged(boolean enabled) {
        refreshState(Boolean.valueOf(enabled));
    }

    public void onPaperModeAvailabilityChanged(boolean available) {
        refreshState();
    }
}
