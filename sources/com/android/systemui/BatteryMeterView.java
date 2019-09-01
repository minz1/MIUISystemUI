package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.statusbar.Icons;
import com.android.systemui.statusbar.phone.BatteryIcon;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.util.DisableStateTracker;

public class BatteryMeterView extends LinearLayout implements DemoMode, BatteryController.BatteryStateChangeCallback, ConfigurationController.ConfigurationListener, DarkIconDispatcher.DarkReceiver {
    private final ImageView mBatteryChargingView;
    private BatteryController mBatteryController;
    private FrameLayout mBatteryDigitalView;
    private final ImageView mBatteryIconView;
    private int mBatteryStyle;
    private int[] mBatteryTextColors;
    private TextView mBatteryTextDigitView;
    private boolean mCharging;
    private float mDarkIntensity;
    private boolean mDemoMode;
    private boolean mDisabled;
    private boolean mExtremePowerSave;
    private boolean mForceShowDigit;
    private int mIconId;
    private int mLevel;
    private boolean mNotchEar;
    private boolean mPowerSave;
    private boolean mQuickCharging;
    private boolean mShowBatteryDigitFull;
    private Rect mTintArea;
    /* access modifiers changed from: private */
    public Runnable mUpdateQuickChargingTask;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mBatteryTextColors = new int[7];
        this.mIconId = R.raw.stat_sys_battery;
        this.mTintArea = new Rect();
        this.mQuickCharging = false;
        this.mForceShowDigit = false;
        this.mNotchEar = false;
        this.mBatteryStyle = 0;
        this.mUpdateQuickChargingTask = new Runnable() {
            public void run() {
                BatteryMeterView.this.updateQuickCharging();
            }
        };
        updateResources();
        setOrientation(0);
        setGravity(8388627);
        addOnAttachStateChangeListener(new DisableStateTracker(0, 2));
        this.mBatteryChargingView = new ImageView(context);
        addView(this.mBatteryChargingView, new LinearLayout.LayoutParams(-2, -1));
        this.mBatteryDigitalView = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.battery_digital_view, null);
        this.mBatteryIconView = (ImageView) this.mBatteryDigitalView.findViewById(R.id.battery_image);
        this.mBatteryTextDigitView = (TextView) this.mBatteryDigitalView.findViewById(R.id.battery_digit);
        this.mBatteryTextDigitView.setTypeface(Typeface.createFromAsset(this.mContext.getAssets(), "fonts/Mitype2018-battery.otf"));
        addView(this.mBatteryDigitalView);
        onDarkChanged(new Rect(), 0.0f, -1);
    }

    private void updateResources() {
        Resources resources = this.mContext.getResources();
        this.mBatteryTextColors[0] = resources.getColor(R.color.status_bar_textColor);
        this.mBatteryTextColors[1] = resources.getColor(R.color.status_bar_textColor_darkmode);
        this.mBatteryTextColors[2] = resources.getColor(R.color.status_bar_battery_digit_textColor);
        this.mBatteryTextColors[3] = resources.getColor(R.color.status_bar_battery_digit_textColor_darkmode);
        this.mBatteryTextColors[4] = resources.getColor(R.color.status_bar_battery_power_save_digit_textColor);
        this.mBatteryTextColors[5] = resources.getColor(R.color.status_bar_battery_power_save_digit_textColor_darkmode);
        this.mBatteryTextColors[6] = resources.getColor(R.color.status_bar_icon_text_color_dark_mode_cts);
        this.mShowBatteryDigitFull = resources.getBoolean(R.bool.show_battery_digit_full);
        if (this.mBatteryTextDigitView != null) {
            int paddingStart = resources.getDimensionPixelSize(R.dimen.statusbar_battery_digit_padding_start);
            int paddingEnd = resources.getDimensionPixelSize(R.dimen.statusbar_battery_digit_padding_end);
            this.mBatteryTextDigitView.setPaddingRelative(paddingStart, resources.getDimensionPixelSize(R.dimen.statusbar_battery_digit_padding_top), paddingEnd, resources.getDimensionPixelSize(R.dimen.statusbar_battery_digit_padding_bottom));
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mBatteryController.addCallback(this);
        updateViews();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBatteryController.removeCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
        removeCallbacks(this.mUpdateQuickChargingTask);
    }

    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        int i;
        if (charging != this.mCharging) {
            this.mCharging = charging;
            notifyChargingStateChanged(this.mCharging);
            BatteryIcon.getInstance(this.mContext).clear();
        }
        if (!charging) {
            this.mQuickCharging = false;
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
        if (Constants.IS_NOTCH && batteryStyle == 2) {
            batteryStyle = 0;
        }
        boolean z = true;
        if (batteryStyle != 1) {
            z = false;
        }
        this.mForceShowDigit = z;
        this.mBatteryStyle = batteryStyle;
        if (batteryStyle != 2) {
            if (this.mBatteryDigitalView != null) {
                this.mBatteryDigitalView.setVisibility(0);
            }
            setDigitViewTextColor();
        } else if (this.mBatteryDigitalView != null) {
            this.mBatteryDigitalView.setVisibility(8);
        }
        this.mIconId = getIconId();
        updateBatteryChargingIcon();
        update();
    }

    public void onPowerSaveChanged(boolean isPowerSave) {
        int i;
        this.mPowerSave = isPowerSave;
        if (this.mIconId != getIconId()) {
            this.mIconId = getIconId();
            ImageView imageView = this.mBatteryIconView;
            Context context = getContext();
            if (this.mCharging) {
                i = R.string.accessibility_battery_level_charging;
            } else {
                i = R.string.accessibility_battery_level;
            }
            imageView.setContentDescription(context.getString(i, new Object[]{Integer.valueOf(this.mLevel)}));
            setDigitViewTextColor();
            update();
        }
    }

    public void onExtremePowerSaveChanged(boolean isExtremePowerSave) {
        int i;
        this.mExtremePowerSave = isExtremePowerSave;
        if (this.mIconId != getIconId()) {
            this.mIconId = getIconId();
            ImageView imageView = this.mBatteryIconView;
            Context context = getContext();
            if (this.mCharging) {
                i = R.string.accessibility_battery_level_charging;
            } else {
                i = R.string.accessibility_battery_level;
            }
            imageView.setContentDescription(context.getString(i, new Object[]{Integer.valueOf(this.mLevel)}));
            setDigitViewTextColor();
            update();
        }
    }

    private void updateViews() {
        updateIconView(this.mBatteryStyle != 2);
    }

    private void updateIconView(boolean isShow) {
        if (isShow) {
            this.mBatteryDigitalView.setVisibility(0);
        } else {
            this.mBatteryDigitalView.setVisibility(8);
        }
    }

    private void updateChargingIconView() {
        if (!this.mDemoMode) {
            this.mBatteryChargingView.setVisibility((this.mNotchEar || (!this.mQuickCharging && (!this.mCharging || this.mBatteryStyle != 2))) ? 8 : 0);
        }
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
        BatteryIcon.getInstance(this.mContext).clear();
        this.mIconId = getIconId();
        update();
    }

    public void onDensityOrFontScaleChanged() {
        scaleBatteryMeterViews();
    }

    private void scaleBatteryMeterViews() {
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();
        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();
        int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        new LinearLayout.LayoutParams((int) (((float) batteryWidth) * iconScaleFactor), (int) (((float) batteryHeight) * iconScaleFactor)).setMargins(0, 0, 0, res.getDimensionPixelSize(R.dimen.battery_margin_bottom));
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        this.mTintArea.set(area);
        this.mDarkIntensity = darkIntensity;
        setDigitViewTextColor();
        updateBatteryChargingIcon();
        update();
    }

    public void setDigitViewTextColor() {
        int i;
        int i2;
        if (Util.showCtsSpecifiedColor()) {
            if (this.mForceShowDigit) {
                boolean dark = DarkIconDispatcherHelper.inDarkMode(this.mTintArea, this.mBatteryTextDigitView, this.mDarkIntensity);
                TextView textView = this.mBatteryTextDigitView;
                if (dark) {
                    i2 = this.mBatteryTextColors[6];
                } else if (this.mPowerSave || this.mExtremePowerSave) {
                    i2 = this.mBatteryTextColors[4];
                } else {
                    i2 = this.mBatteryTextColors[2];
                }
                textView.setTextColor(i2);
            }
        } else if (this.mForceShowDigit) {
            boolean dark2 = DarkIconDispatcherHelper.inDarkMode(this.mTintArea, this.mBatteryTextDigitView, this.mDarkIntensity);
            TextView textView2 = this.mBatteryTextDigitView;
            if (dark2) {
                if (this.mPowerSave || this.mExtremePowerSave) {
                    i = this.mBatteryTextColors[5];
                } else {
                    i = this.mBatteryTextColors[3];
                }
            } else if (this.mPowerSave || this.mExtremePowerSave) {
                i = this.mBatteryTextColors[4];
            } else {
                i = this.mBatteryTextColors[2];
            }
            textView2.setTextColor(i);
        }
    }

    public void update() {
        this.mBatteryIconView.setImageDrawable(this.mDemoMode ? getDemoModeIcon(this.mBatteryIconView) : getIcon(this.mBatteryIconView, this.mIconId));
        this.mBatteryIconView.setImageLevel(this.mDemoMode ? 100 : this.mLevel);
        this.mBatteryTextDigitView.setText(String.valueOf(this.mLevel));
        this.mBatteryTextDigitView.setVisibility((!this.mForceShowDigit || (!this.mShowBatteryDigitFull && this.mLevel == 100) || (!this.mShowBatteryDigitFull && this.mCharging)) ? 8 : 0);
        updateBatteryChargingIcon();
        invalidate();
    }

    private int getIconId() {
        return this.mCharging ? this.mForceShowDigit ? R.raw.stat_sys_battery_charge_digit : R.raw.stat_sys_battery_charge : (this.mPowerSave != 0 || this.mExtremePowerSave) ? this.mForceShowDigit ? R.raw.stat_sys_battery_power_save_digit : R.raw.stat_sys_battery_power_save : this.mForceShowDigit ? R.raw.stat_sys_battery_digital : R.raw.stat_sys_battery;
    }

    /* access modifiers changed from: protected */
    public Drawable getIcon(ImageView view, int resId) {
        LevelListDrawable levelListDrawable;
        LevelListDrawable levelListDrawable2;
        LevelListDrawable levelListDrawable3;
        LevelListDrawable levelListDrawable4;
        LevelListDrawable levelListDrawable5;
        LevelListDrawable levelListDrawable6;
        boolean dark = DarkIconDispatcherHelper.inDarkMode(this.mTintArea, view, this.mDarkIntensity);
        switch (resId) {
            case R.raw.stat_sys_battery /*2131755012*/:
                if (dark) {
                    levelListDrawable = BatteryIcon.getInstance(this.mContext).getGraphicIconDarkMode(this.mLevel);
                } else {
                    levelListDrawable = BatteryIcon.getInstance(this.mContext).getGraphicIcon(this.mLevel);
                }
                return levelListDrawable;
            case R.raw.stat_sys_battery_charge /*2131755013*/:
                if (dark) {
                    levelListDrawable2 = BatteryIcon.getInstance(this.mContext).getGraphicChargeIconDarkMode(this.mLevel);
                } else {
                    levelListDrawable2 = BatteryIcon.getInstance(this.mContext).getGraphicChargeIcon(this.mLevel);
                }
                return levelListDrawable2;
            case R.raw.stat_sys_battery_charge_digit /*2131755015*/:
                if (dark) {
                    levelListDrawable3 = BatteryIcon.getInstance(this.mContext).getGraphicChargeDigitIconDarkMode(this.mLevel);
                } else {
                    levelListDrawable3 = BatteryIcon.getInstance(this.mContext).getGraphicChargeDigitIcon(this.mLevel);
                }
                return levelListDrawable3;
            case R.raw.stat_sys_battery_digital /*2131755018*/:
                if (dark) {
                    levelListDrawable4 = BatteryIcon.getInstance(this.mContext).getGraphicDigitalIconDarkMode(this.mLevel);
                } else {
                    levelListDrawable4 = BatteryIcon.getInstance(this.mContext).getGraphicDigitalIcon(this.mLevel);
                }
                return levelListDrawable4;
            case R.raw.stat_sys_battery_power_save /*2131755020*/:
                if (dark) {
                    levelListDrawable5 = BatteryIcon.getInstance(this.mContext).getGraphicPowerSaveIconDarkMode(this.mLevel);
                } else {
                    levelListDrawable5 = BatteryIcon.getInstance(this.mContext).getGraphicPowerSaveIcon(this.mLevel);
                }
                return levelListDrawable5;
            case R.raw.stat_sys_battery_power_save_digit /*2131755022*/:
                if (dark) {
                    levelListDrawable6 = BatteryIcon.getInstance(this.mContext).getGraphicPowerSaveDigitIconDarkMode(this.mLevel);
                } else {
                    levelListDrawable6 = BatteryIcon.getInstance(this.mContext).getGraphicPowerSaveDigitIcon(this.mLevel);
                }
                return levelListDrawable6;
            default:
                return null;
        }
    }

    private Drawable getDemoModeIcon(ImageView view) {
        if (DarkIconDispatcherHelper.inDarkMode(this.mTintArea, view, this.mDarkIntensity)) {
            return BatteryIcon.getInstance(this.mContext).getGraphicIconDarkMode(100);
        }
        return BatteryIcon.getInstance(this.mContext).getGraphicIcon(100);
    }

    /* access modifiers changed from: protected */
    public void updateVisibility() {
        if (!this.mDemoMode) {
            setVisibility(!this.mDisabled ? 0 : 8);
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            setVisibility(0);
            update();
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            updateVisibility();
            update();
        }
    }

    private void notifyChargingStateChanged(final boolean charging) {
        new AsyncTask<Void, Void, Boolean>() {
            /* access modifiers changed from: protected */
            public Boolean doInBackground(Void... params) {
                return Boolean.valueOf(ChargingUtils.isQuickCharging(BatteryMeterView.this.mContext));
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean quickCharging) {
                if (charging) {
                    BatteryMeterView.this.updateQuickChargingDelayed(3000);
                    BatteryMeterView.this.updateQuickChargingDelayed(20000);
                    BatteryMeterView.this.updateQuickChargingDelayed(120000);
                    return;
                }
                BatteryMeterView.this.updateQuickCharging(false);
                BatteryMeterView.this.removeCallbacks(BatteryMeterView.this.mUpdateQuickChargingTask);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private void updateBatteryChargingIcon() {
        if (this.mQuickCharging || this.mCharging) {
            updateIcon(this.mBatteryChargingView, getChangingIconId());
            updateChargingIconView();
            return;
        }
        this.mBatteryChargingView.setVisibility(8);
    }

    private int getChangingIconId() {
        if (this.mQuickCharging) {
            return R.drawable.stat_sys_quick_charging;
        }
        return R.drawable.stat_sys_battery_charging;
    }

    private void updateIcon(ImageView icon, int drawableId) {
        boolean isDarkMode = DarkIconDispatcherHelper.inDarkMode(this.mTintArea, icon, this.mDarkIntensity);
        icon.setImageResource(Icons.get(Integer.valueOf(drawableId), isDarkMode));
        Drawable drawable = icon.getDrawable();
        if (drawable == null) {
            return;
        }
        if (!isDarkMode || !Util.showCtsSpecifiedColor()) {
            drawable.setColorFilter(null);
        } else {
            drawable.setColorFilter(this.mBatteryTextColors[6], PorterDuff.Mode.SRC_IN);
        }
    }

    public void updateQuickCharging() {
        new AsyncTask<Void, Void, Boolean>() {
            /* access modifiers changed from: protected */
            public Boolean doInBackground(Void... params) {
                return Boolean.valueOf(ChargingUtils.isQuickCharging(BatteryMeterView.this.mContext));
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean quickCharging) {
                BatteryMeterView.this.updateQuickCharging(quickCharging.booleanValue());
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void updateQuickChargingDelayed(long delayMillis) {
        postDelayed(this.mUpdateQuickChargingTask, delayMillis);
    }

    public void updateQuickCharging(boolean quickCharging) {
        this.mQuickCharging = quickCharging;
        updateBatteryChargingIcon();
    }

    public void setNortchEar(boolean isNorthEar) {
        this.mNotchEar = isNorthEar;
        onBatteryStyleChanged(this.mBatteryStyle);
    }
}
