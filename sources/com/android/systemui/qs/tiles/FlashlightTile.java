package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.FlashlightController;

public class FlashlightTile extends QSTileImpl<QSTile.BooleanState> implements FlashlightController.FlashlightListener {
    private final FlashlightController mFlashlightController = ((FlashlightController) Dependency.get(FlashlightController.class));

    public FlashlightTile(QSHost host) {
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
            this.mFlashlightController.addCallback(this);
        } else {
            this.mFlashlightController.removeCallback(this);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
    }

    public Intent getLongClickIntent() {
        return new Intent("android.media.action.STILL_IMAGE_CAMERA");
    }

    public boolean isAvailable() {
        return this.mFlashlightController.hasFlashlight();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (!ActivityManager.isUserAMonkey()) {
            this.mFlashlightController.setFlashlight(!((QSTile.BooleanState) this.mState).value);
            refreshState();
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_flashlight_label);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.quick_settings_flashlight_label);
        boolean isAvailable = this.mFlashlightController.isAvailable();
        int i = R.drawable.ic_qs_flashlight_disabled;
        if (!isAvailable) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_flashlight_disabled);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_flashlight_unavailable);
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
            state.value = this.mFlashlightController.isEnabled();
        }
        state.state = state.value ? 2 : 1;
        if (state.value) {
            i = R.drawable.ic_qs_flashlight_enabled;
        }
        state.icon = QSTileImpl.ResourceIcon.get(i);
        StringBuilder sb = new StringBuilder();
        sb.append(state.label);
        sb.append(",");
        sb.append(this.mContext.getString(state.value ? R.string.switch_bar_on : R.string.switch_bar_off));
        state.contentDescription = sb.toString();
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 119;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
    }

    public void onFlashlightChanged(boolean enabled) {
        refreshState(Boolean.valueOf(enabled));
    }

    public void onFlashlightError() {
        refreshState(false);
    }

    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }
}
