package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.Constants;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryIndicator extends ImageView implements DemoMode, BatteryController.BatteryStateChangeCallback {
    private int mClipWidth;
    private boolean mDemoMode;
    protected boolean mDisabled = false;
    protected int mDisplayWidth;
    protected boolean mIsCharging;
    protected boolean mIsExtremePowerSave;
    protected boolean mIsPowerSave;
    protected final int mLowLevel = this.mContext.getResources().getInteger(285736963);
    protected int mPowerLevel;
    private boolean mShowBatteryIndicator;

    public BatteryIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateDisplaySize();
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((BatteryController) Dependency.get(BatteryController.class)).addCallback(this);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((BatteryController) Dependency.get(BatteryController.class)).removeCallback(this);
    }

    public void onPowerSaveChanged(boolean isPowerSave) {
        if (isPowerSave != this.mIsPowerSave) {
            this.mIsPowerSave = isPowerSave;
            update();
        }
    }

    public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
        if (isExtremePowerSave != this.mIsExtremePowerSave) {
            this.mIsExtremePowerSave = isExtremePowerSave;
            update();
        }
    }

    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        if (charging != this.mIsCharging || this.mPowerLevel != level) {
            this.mIsCharging = charging;
            this.mPowerLevel = level;
            update();
        }
    }

    public void onBatteryStyleChanged(int batteryStyle) {
        this.mShowBatteryIndicator = batteryStyle == 2;
        update();
    }

    public void update() {
        updateVisibility();
        if (getVisibility() == 0) {
            updateDrawable();
        }
    }

    /* access modifiers changed from: protected */
    public void updateDrawable() {
        if (!this.mDemoMode) {
            int newClipWidth = (this.mDisplayWidth * this.mPowerLevel) / 100;
            if (this.mClipWidth != newClipWidth) {
                this.mClipWidth = newClipWidth;
                invalidate();
            }
            int resId = R.drawable.battery_indicator;
            if (!this.mIsCharging) {
                if (this.mIsPowerSave || this.mIsExtremePowerSave) {
                    resId = R.drawable.battery_indicator_power_save;
                } else if (this.mPowerLevel < this.mLowLevel) {
                    resId = R.drawable.battery_indicator_low;
                }
            }
            setImageResource(resId);
            Drawable drawable = getDrawable();
            if (drawable != null) {
                if (Util.showCtsSpecifiedColor()) {
                    drawable.setColorFilter(getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
                } else {
                    drawable.setColorFilter(null);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateDisplaySize() {
        this.mDisplayWidth = getMeasuredWidth();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            updateDisplaySize();
            postUpdate();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateDisplaySize();
        postUpdate();
    }

    public void onDraw(Canvas canvas) {
        canvas.save();
        updateCanvas(canvas);
        super.onDraw(canvas);
        canvas.restore();
    }

    /* access modifiers changed from: protected */
    public void updateCanvas(Canvas canvas) {
        if (getLayoutDirection() == 0) {
            canvas.clipRect(this.mLeft, this.mTop, this.mLeft + this.mClipWidth, this.mBottom);
        } else {
            canvas.clipRect(this.mRight - this.mClipWidth, this.mTop, this.mRight, this.mBottom);
        }
    }

    /* access modifiers changed from: protected */
    public void updateVisibility() {
        if (!this.mDemoMode) {
            if (!this.mShowBatteryIndicator || this.mDisabled || Constants.IS_NOTCH) {
                setVisibility(8);
                clearAnimation();
            } else {
                setVisibility(0);
            }
        }
    }

    private void postUpdate() {
        post(new Runnable() {
            public void run() {
                BatteryIndicator.this.update();
            }
        });
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            setVisibility(8);
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            update();
        }
    }
}
