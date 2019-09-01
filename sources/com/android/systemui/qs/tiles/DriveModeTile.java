package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import miui.securityspace.CrossUserUtils;

public class DriveModeTile extends QSTileImpl<QSTile.BooleanState> {
    private static boolean mMiuiLabDriveModeOn;
    private ContentObserver mDriveModeObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            Log.d("SystemUI.DriveMode", "drive mode change detected");
            DriveModeTile.this.refreshState();
        }
    };
    private final ContentResolver mResolver = this.mContext.getContentResolver();

    public DriveModeTile(QSHost host) {
        super(host);
        mMiuiLabDriveModeOn = -1 != Settings.System.getIntForUser(this.mResolver, "drive_mode_drive_mode", -1, -2);
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public boolean isAvailable() {
        return !Constants.IS_INTERNATIONAL && !Constants.IS_TABLET && CrossUserUtils.getCurrentUserId() == 0;
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mResolver.registerContentObserver(Settings.System.getUriFor("drive_mode_drive_mode"), false, this.mDriveModeObserver, -1);
        } else {
            this.mResolver.unregisterContentObserver(this.mDriveModeObserver);
        }
    }

    public Intent getLongClickIntent() {
        if (!this.mHost.isDriveModeInstalled()) {
            return getMiuiLabSettingsIntent();
        }
        if (!mMiuiLabDriveModeOn) {
            mMiuiLabDriveModeOn = -1 != Settings.System.getIntForUser(this.mResolver, "drive_mode_drive_mode", -1, -2);
            if (!mMiuiLabDriveModeOn) {
                return getMiuiLabSettingsIntent();
            }
        }
        return longClickDriveModeIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (!mMiuiLabDriveModeOn) {
            mMiuiLabDriveModeOn = -1 != Settings.System.getIntForUser(this.mResolver, "drive_mode_drive_mode", -1, -2);
        }
        if (!this.mHost.isDriveModeInstalled()) {
            transitionMiuiLabSettings();
        } else if (!mMiuiLabDriveModeOn) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.xiaomi.drivemode", "com.xiaomi.drivemode.MiuiLabDriveModeActivity"));
            intent.addFlags(268435456);
            intent.putExtra("EXTRA_START_MODE", true);
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
        } else if (!((QSTile.BooleanState) this.mState).value) {
            startDriveModeActivity();
        } else {
            Settings.System.putIntForUser(this.mResolver, "drive_mode_drive_mode", 0, -2);
            this.mHost.collapsePanels();
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_drivemode_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        Log.d("SystemUI.DriveMode", "drive mode handleUpdateState");
        boolean z = false;
        if (Settings.System.getIntForUser(this.mResolver, "drive_mode_drive_mode", 0, -2) > 0) {
            z = true;
        }
        state.value = z;
        state.label = this.mContext.getString(R.string.quick_settings_drivemode_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_drive_enabled);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_drive_disabled);
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

    private void startDriveModeActivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xiaomi.drivemode", "com.xiaomi.drivemode.UserGuideActivity"));
        intent.addFlags(268435456);
        intent.putExtra("EXTRA_START_MODE", true);
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
    }

    private Intent longClickDriveModeIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.xiaomi.drivemode/.DriveModeSettingsActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }

    private Intent getMiuiLabSettingsIntent() {
        Intent intent = new Intent();
        intent.setFlags(335544320);
        intent.setAction("android.intent.action.MAIN");
        intent.putExtra(":android:show_fragment", "com.android.settings.MiuiLabSettings");
        intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
        return intent;
    }

    public static void leaveDriveMode(Context context) {
        Settings.System.putIntForUser(context.getContentResolver(), "drive_mode_drive_mode", -1, -2);
        Intent intent = new Intent();
        intent.setAction("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode");
        context.sendBroadcast(intent);
    }

    private void transitionMiuiLabSettings() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(getMiuiLabSettingsIntent(), 0);
    }
}
