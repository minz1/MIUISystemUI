package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import miui.os.DeviceFeature;

public class AutoBrightnessTile extends QSTileImpl<QSTile.BooleanState> {
    private static final boolean SUPPORT_AUTO_BRIGHTNESS_OPTIMIZE = DeviceFeature.SUPPORT_AUTO_BRIGHTNESS_OPTIMIZE;
    private boolean mAutoBrightnessAvailable = this.mResource.getBoolean(285868033);
    private boolean mAutoBrightnessMode;
    private ContentObserver mAutoBrightnessObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            AutoBrightnessTile.this.refreshState();
        }
    };
    private int mCurrentUserId = ActivityManager.getCurrentUser();
    private final ContentResolver mResolver = this.mContext.getContentResolver();
    private final Resources mResource = this.mContext.getResources();

    public AutoBrightnessTile(QSHost host) {
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
            if (!SUPPORT_AUTO_BRIGHTNESS_OPTIMIZE) {
                this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mAutoBrightnessObserver, this.mCurrentUserId);
                this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mAutoBrightnessObserver, this.mCurrentUserId);
            }
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mAutoBrightnessObserver, this.mCurrentUserId);
            return;
        }
        this.mResolver.unregisterContentObserver(this.mAutoBrightnessObserver);
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    public Intent getLongClickIntent() {
        return longClickAutoBrightnessIntent();
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        int i = 0;
        if (this.mAutoBrightnessMode) {
            this.mAutoBrightnessMode = false;
        } else {
            this.mAutoBrightnessMode = this.mAutoBrightnessAvailable;
        }
        String str = this.TAG;
        Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + this.mAutoBrightnessMode);
        ContentResolver contentResolver = this.mResolver;
        if (this.mAutoBrightnessMode) {
            i = 1;
        }
        Settings.System.putIntForUser(contentResolver, "screen_brightness_mode", i, this.mCurrentUserId);
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_autobrightness_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        queryAutoBrightnessStatus();
        state.value = this.mAutoBrightnessMode;
        state.label = this.mContext.getString(R.string.quick_settings_autobrightness_label);
        if (state.value) {
            state.state = 2;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_brightness_auto);
        } else {
            state.state = 1;
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_brightness_manual);
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

    private Intent longClickAutoBrightnessIntent() {
        ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.display.BrightnessActivity");
        if (component == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(component);
        intent.setFlags(335544320);
        return intent;
    }

    private void queryAutoBrightnessStatus() {
        boolean z = false;
        if (this.mAutoBrightnessAvailable && 1 == Settings.System.getIntForUser(this.mResolver, "screen_brightness_mode", 0, this.mCurrentUserId)) {
            z = true;
        }
        this.mAutoBrightnessMode = z;
    }
}
