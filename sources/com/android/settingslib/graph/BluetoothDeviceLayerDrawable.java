package com.android.settingslib.graph;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import com.android.settingslib.R;
import com.android.settingslib.Utils;

public class BluetoothDeviceLayerDrawable extends LayerDrawable {
    private BluetoothDeviceLayerDrawableState mState;

    static class BatteryMeterDrawable extends BatteryMeterDrawableBase {
        private final float mAspectRatio;
        int mFrameColor;

        public BatteryMeterDrawable(Context context, int frameColor, int batteryLevel) {
            super(context, frameColor);
            Resources resources = context.getResources();
            this.mButtonHeightFraction = resources.getFraction(R.fraction.bt_battery_button_height_fraction, 1, 1);
            this.mAspectRatio = resources.getFraction(R.fraction.bt_battery_ratio_fraction, 1, 1);
            setColorFilter(new PorterDuffColorFilter(Utils.getColorAttr(context, 16843817), PorterDuff.Mode.SRC_IN));
            setBatteryLevel(batteryLevel);
            this.mFrameColor = frameColor;
        }

        /* access modifiers changed from: protected */
        public float getAspectRatio() {
            return this.mAspectRatio;
        }

        /* access modifiers changed from: protected */
        public float getRadiusRatio() {
            return 0.0f;
        }
    }

    private static class BluetoothDeviceLayerDrawableState extends Drawable.ConstantState {
        int batteryLevel;
        Context context;
        float iconScale;
        int resId;

        public BluetoothDeviceLayerDrawableState(Context context2, int resId2, int batteryLevel2, float iconScale2) {
            this.context = context2;
            this.resId = resId2;
            this.batteryLevel = batteryLevel2;
            this.iconScale = iconScale2;
        }

        public Drawable newDrawable() {
            return BluetoothDeviceLayerDrawable.createLayerDrawable(this.context, this.resId, this.batteryLevel, this.iconScale);
        }

        public int getChangingConfigurations() {
            return 0;
        }
    }

    private BluetoothDeviceLayerDrawable(Drawable[] layers) {
        super(layers);
    }

    public static BluetoothDeviceLayerDrawable createLayerDrawable(Context context, int resId, int batteryLevel, float iconScale) {
        Drawable deviceDrawable = context.getDrawable(resId);
        BatteryMeterDrawable batteryDrawable = new BatteryMeterDrawable(context, context.getColor(R.color.meter_background_color), batteryLevel);
        int pad = context.getResources().getDimensionPixelSize(R.dimen.bt_battery_padding);
        batteryDrawable.setPadding(pad, pad, pad, pad);
        BluetoothDeviceLayerDrawable drawable = new BluetoothDeviceLayerDrawable(new Drawable[]{deviceDrawable, batteryDrawable});
        drawable.setLayerGravity(0, 8388611);
        drawable.setLayerInsetStart(1, deviceDrawable.getIntrinsicWidth());
        drawable.setLayerInsetTop(1, (int) (((float) deviceDrawable.getIntrinsicHeight()) * (1.0f - iconScale)));
        drawable.setConstantState(context, resId, batteryLevel, iconScale);
        return drawable;
    }

    public void setConstantState(Context context, int resId, int batteryLevel, float iconScale) {
        this.mState = new BluetoothDeviceLayerDrawableState(context, resId, batteryLevel, iconScale);
    }

    public Drawable.ConstantState getConstantState() {
        return this.mState;
    }
}
