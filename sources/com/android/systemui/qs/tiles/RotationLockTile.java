package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.RotationLockController;

public class RotationLockTile extends QSTileImpl<QSTile.BooleanState> {
    private final RotationLockController.RotationLockControllerCallback mCallback = new RotationLockController.RotationLockControllerCallback() {
        public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
            RotationLockTile.this.refreshState(Boolean.valueOf(rotationLocked));
        }
    };
    private final RotationLockController mController = ((RotationLockController) Dependency.get(RotationLockController.class));

    public RotationLockTile(QSHost host) {
        super(host);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (this.mController != null) {
            if (listening) {
                this.mController.addCallback(this.mCallback);
            } else {
                this.mController.removeCallback(this.mCallback);
            }
        }
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.DISPLAY_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (this.mController != null) {
            boolean newState = !((QSTile.BooleanState) this.mState).value;
            this.mController.setRotationLocked(newState);
            refreshState(Boolean.valueOf(newState));
        }
    }

    public CharSequence getTileLabel() {
        return ((QSTile.BooleanState) getState()).label;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int i;
        if (this.mController != null) {
            boolean rotationLocked = this.mController.isRotationLocked();
            state.value = rotationLocked;
            state.label = this.mContext.getString(285802503);
            if (!rotationLocked) {
                i = R.drawable.ic_qs_auto_rotate_enabled;
            } else {
                i = R.drawable.ic_qs_auto_rotate_disabled;
            }
            state.icon = QSTileImpl.ResourceIcon.get(i);
            state.contentDescription = getAccessibilityString(rotationLocked);
            state.expandedAccessibilityClassName = Switch.class.getName();
            state.state = state.value ? 2 : 1;
        }
    }

    public static boolean isCurrentOrientationLockPortrait(RotationLockController controller, Context context) {
        int lockOrientation = controller.getRotationLockOrientation();
        boolean z = false;
        if (lockOrientation == 0) {
            if (context.getResources().getConfiguration().orientation != 2) {
                z = true;
            }
            return z;
        }
        if (lockOrientation != 2) {
            z = true;
        }
        return z;
    }

    public int getMetricsCategory() {
        return 123;
    }

    private String getAccessibilityString(boolean locked) {
        String str;
        if (!locked) {
            return this.mContext.getString(R.string.accessibility_quick_settings_rotation);
        }
        StringBuilder sb = new StringBuilder();
        Context context = this.mContext;
        Object[] objArr = new Object[1];
        if (isCurrentOrientationLockPortrait(this.mController, this.mContext)) {
            str = this.mContext.getString(R.string.quick_settings_rotation_locked_portrait_label);
        } else {
            str = this.mContext.getString(R.string.quick_settings_rotation_locked_landscape_label);
        }
        objArr[0] = str;
        sb.append(context.getString(R.string.accessibility_quick_settings_rotation_value, objArr));
        sb.append(",");
        sb.append(this.mContext.getString(R.string.accessibility_quick_settings_rotation));
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        return getAccessibilityString(((QSTile.BooleanState) this.mState).value);
    }
}
