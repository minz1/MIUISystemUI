package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;

public class ChargingView extends ImageView implements BatteryController.BatteryStateChangeCallback, ConfigurationController.ConfigurationListener {
    private BatteryController mBatteryController;
    private boolean mCharging;
    private boolean mDark;
    private int mImageResource;

    public ChargingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{16843033});
        int srcResId = a.getResourceId(0, 0);
        if (srcResId != 0) {
            this.mImageResource = srcResId;
        }
        a.recycle();
        updateVisibility();
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
        this.mCharging = charging;
        updateVisibility();
    }

    public void onBatteryStyleChanged(int batteryStyle) {
    }

    public void onPowerSaveChanged(boolean isPowerSave) {
    }

    public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    public void onDensityOrFontScaleChanged() {
        setImageResource(this.mImageResource);
    }

    private void updateVisibility() {
        setVisibility((!this.mCharging || !this.mDark) ? 4 : 0);
    }
}
