package com.android.keyguard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.BatteryIcon;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.util.DisableStateTracker;

public class AODBatteryMeterView extends LinearLayout implements BatteryController.BatteryStateChangeCallback, ConfigurationController.ConfigurationListener {
    private BatteryController mBatteryController;
    private ImageView mBatteryIconView;
    private TextView mBatteryTextDigitView;
    private boolean mCharging;
    private int mIconId;
    private int mLevel;

    public AODBatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public AODBatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AODBatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIconId = R.raw.stat_sys_battery;
        setOrientation(0);
        setGravity(8388627);
        addOnAttachStateChangeListener(new DisableStateTracker(0, 2));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBatteryIconView = (ImageView) findViewById(R.id.aod_battery_icon);
        this.mBatteryTextDigitView = (TextView) findViewById(R.id.aod_battery_digital);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mBatteryController.addCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBatteryController.removeCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        int i;
        if (charging != this.mCharging) {
            this.mCharging = charging;
            BatteryIcon.getInstance(this.mContext).clear();
        }
        if (this.mIconId != getIconId() || level != this.mLevel) {
            this.mLevel = level;
            this.mCharging = charging;
            this.mIconId = getIconId();
            ImageView imageView = this.mBatteryIconView;
            Context context = getContext();
            if (charging) {
                i = R.string.accessibility_battery_level_charging;
            } else {
                i = R.string.accessibility_battery_level;
            }
            imageView.setContentDescription(context.getString(i, new Object[]{Integer.valueOf(level)}));
            update();
        }
    }

    public void onBatteryStyleChanged(int batteryStyle) {
    }

    public void onPowerSaveChanged(boolean isPowerSave) {
    }

    public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        BatteryIcon.getInstance(this.mContext).clear();
        this.mIconId = getIconId();
        update();
    }

    public void onDensityOrFontScaleChanged() {
    }

    public void update() {
        this.mBatteryIconView.setImageDrawable(getIcon(this.mIconId));
        this.mBatteryIconView.setImageLevel(this.mLevel);
        TextView textView = this.mBatteryTextDigitView;
        textView.setText(String.valueOf(this.mLevel) + "%");
        invalidate();
    }

    private int getIconId() {
        if (this.mCharging) {
            return R.raw.stat_sys_battery_charge;
        }
        return R.raw.stat_sys_battery;
    }

    /* access modifiers changed from: protected */
    public Drawable getIcon(int resId) {
        switch (resId) {
            case R.raw.stat_sys_battery /*2131755012*/:
                return BatteryIcon.getInstance(this.mContext).getGraphicIcon(this.mLevel);
            case R.raw.stat_sys_battery_charge /*2131755013*/:
                return BatteryIcon.getInstance(this.mContext).getGraphicChargeIcon(this.mLevel);
            default:
                return null;
        }
    }
}
