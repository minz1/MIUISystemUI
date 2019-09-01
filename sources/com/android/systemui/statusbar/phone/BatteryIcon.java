package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.util.SparseArray;
import android.util.TypedValue;
import com.android.systemui.R;
import com.android.systemui.Util;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BatteryIcon {
    private static BatteryIcon sBatteryIcon;
    private final int BATTERY_RANGE_LOAD = 10;
    private int mBatteryColumns;
    private int mChargeDarkLevel = -1;
    private int mChargeDigitDarkLevel = -1;
    private int mChargeDigitLevel = -1;
    private int mChargeLevel = -1;
    private Context mContext;
    private int mDarkLevel = -1;
    private int mDigitalDarkLevel = -1;
    private int mDigitalLevel = -1;
    private LevelListDrawable mGraphicChargeDigitIcon;
    private LevelListDrawable mGraphicChargeDigitIconDarkMode;
    private LevelListDrawable mGraphicChargeIcon;
    private LevelListDrawable mGraphicChargeIconDarkMode;
    private LevelListDrawable mGraphicDigitalIcon;
    private LevelListDrawable mGraphicDigitalIconDarkMode;
    private LevelListDrawable mGraphicIcon;
    private LevelListDrawable mGraphicIconDarkMode;
    private LevelListDrawable mGraphicPowerSaveDigitIcon;
    private LevelListDrawable mGraphicPowerSaveDigitIconDarkMode;
    private LevelListDrawable mGraphicPowerSaveIcon;
    private LevelListDrawable mGraphicPowerSaveIconDarkMode;
    private SparseArray<ArrayList<Drawable>> mGraphicRes2Drawables = new SparseArray<>();
    private int mLevel = -1;
    private int mPowerSaveDarkLevel = -1;
    private int mPowerSaveDigitDarkLevel = -1;
    private int mPowerSaveDigitLevel = -1;
    private int mPowerSaveLevel = -1;

    public static BatteryIcon getInstance(Context context) {
        if (sBatteryIcon == null) {
            sBatteryIcon = new BatteryIcon(context);
        }
        return sBatteryIcon;
    }

    private BatteryIcon(Context context) {
        this.mContext = context;
        this.mBatteryColumns = this.mContext.getResources().getInteger(R.integer.battery_columns);
    }

    public LevelListDrawable getGraphicIcon(int level) {
        if (this.mLevel == -1 || this.mLevel - level > 10 || this.mLevel - level < 0) {
            this.mGraphicIcon = generateIcon(R.raw.stat_sys_battery, level, false);
            this.mLevel = level;
        }
        return this.mGraphicIcon;
    }

    public LevelListDrawable getGraphicIconDarkMode(int level) {
        if (this.mDarkLevel == -1 || this.mDarkLevel - level > 10 || this.mDarkLevel - level < 0) {
            this.mGraphicIconDarkMode = generateIcon(R.raw.stat_sys_battery_darkmode, level, false);
            this.mDarkLevel = level;
        }
        if (Util.showCtsSpecifiedColor()) {
            this.mGraphicIconDarkMode.setColorFilter(this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
        } else {
            this.mGraphicIconDarkMode.setColorFilter(null);
        }
        return this.mGraphicIconDarkMode;
    }

    public LevelListDrawable getGraphicDigitalIcon(int level) {
        if (this.mDigitalLevel == -1 || this.mDigitalLevel - level > 10 || this.mDigitalLevel - level < 0) {
            this.mGraphicDigitalIcon = generateIcon(R.raw.stat_sys_battery_digital, level, false);
            this.mDigitalLevel = level;
        }
        return this.mGraphicDigitalIcon;
    }

    public LevelListDrawable getGraphicDigitalIconDarkMode(int level) {
        if (this.mDigitalDarkLevel == -1 || this.mDigitalDarkLevel - level > 10 || this.mDigitalDarkLevel - level < 0) {
            this.mGraphicDigitalIconDarkMode = generateIcon(R.raw.stat_sys_battery_digital_darkmode, level, false);
            this.mDigitalDarkLevel = level;
        }
        if (Util.showCtsSpecifiedColor()) {
            this.mGraphicDigitalIconDarkMode.setColorFilter(this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
        } else {
            this.mGraphicDigitalIconDarkMode.setColorFilter(null);
        }
        return this.mGraphicDigitalIconDarkMode;
    }

    public LevelListDrawable getGraphicChargeIcon(int level) {
        if (this.mChargeLevel == -1 || level - this.mChargeLevel > 10 || level - this.mChargeLevel < 0) {
            this.mGraphicChargeIcon = generateIcon(R.raw.stat_sys_battery_charge, level, true);
            this.mChargeLevel = level;
        }
        return this.mGraphicChargeIcon;
    }

    public LevelListDrawable getGraphicChargeIconDarkMode(int level) {
        if (this.mChargeDarkLevel == -1 || level - this.mChargeDarkLevel > 10 || level - this.mChargeDarkLevel < 0) {
            this.mGraphicChargeIconDarkMode = generateIcon(R.raw.stat_sys_battery_charge_darkmode, level, true);
            this.mChargeDarkLevel = level;
        }
        if (Util.showCtsSpecifiedColor()) {
            this.mGraphicChargeIconDarkMode.setColorFilter(this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
        } else {
            this.mGraphicChargeIconDarkMode.setColorFilter(null);
        }
        return this.mGraphicChargeIconDarkMode;
    }

    public LevelListDrawable getGraphicChargeDigitIcon(int level) {
        if (this.mChargeDigitLevel == -1 || level - this.mChargeDigitLevel > 10 || level - this.mChargeDigitLevel < 0) {
            this.mGraphicChargeDigitIcon = generateIcon(R.raw.stat_sys_battery_charge_digit, level, true);
            this.mChargeDigitLevel = level;
        }
        return this.mGraphicChargeDigitIcon;
    }

    public LevelListDrawable getGraphicChargeDigitIconDarkMode(int level) {
        if (this.mChargeDigitDarkLevel == -1 || level - this.mChargeDigitDarkLevel > 10 || level - this.mChargeDigitDarkLevel < 0) {
            this.mGraphicChargeDigitIconDarkMode = generateIcon(R.raw.stat_sys_battery_charge_digit_darkmode, level, true);
            this.mChargeDigitDarkLevel = level;
        }
        if (Util.showCtsSpecifiedColor()) {
            this.mGraphicChargeDigitIconDarkMode.setColorFilter(this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
        } else {
            this.mGraphicChargeDigitIconDarkMode.setColorFilter(null);
        }
        return this.mGraphicChargeDigitIconDarkMode;
    }

    public LevelListDrawable getGraphicPowerSaveIcon(int level) {
        if (this.mPowerSaveLevel == -1 || level - this.mPowerSaveLevel > 10 || level - this.mPowerSaveLevel < 0) {
            this.mGraphicPowerSaveIcon = generateIcon(R.raw.stat_sys_battery_power_save, level, true);
            this.mPowerSaveLevel = level;
        }
        return this.mGraphicPowerSaveIcon;
    }

    public LevelListDrawable getGraphicPowerSaveIconDarkMode(int level) {
        if (this.mPowerSaveDarkLevel == -1 || level - this.mPowerSaveDarkLevel > 10 || level - this.mPowerSaveDarkLevel < 0) {
            this.mGraphicPowerSaveIconDarkMode = generateIcon(R.raw.stat_sys_battery_power_save_darkmode, level, true);
            this.mPowerSaveDarkLevel = level;
        }
        if (Util.showCtsSpecifiedColor()) {
            this.mGraphicPowerSaveIconDarkMode.setColorFilter(this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
        } else {
            this.mGraphicPowerSaveIconDarkMode.setColorFilter(null);
        }
        return this.mGraphicPowerSaveIconDarkMode;
    }

    public LevelListDrawable getGraphicPowerSaveDigitIcon(int level) {
        if (this.mPowerSaveDigitLevel == -1 || level - this.mPowerSaveDigitLevel > 10 || level - this.mPowerSaveDigitLevel < 0) {
            this.mGraphicPowerSaveDigitIcon = generateIcon(R.raw.stat_sys_battery_power_save_digit, level, true);
            this.mPowerSaveDigitLevel = level;
        }
        return this.mGraphicPowerSaveDigitIcon;
    }

    public LevelListDrawable getGraphicPowerSaveDigitIconDarkMode(int level) {
        if (this.mPowerSaveDigitDarkLevel == -1 || level - this.mPowerSaveDigitDarkLevel > 10 || level - this.mPowerSaveDigitDarkLevel < 0) {
            this.mGraphicPowerSaveDigitIconDarkMode = generateIcon(R.raw.stat_sys_battery_power_save_digit_darkmode, level, true);
            this.mPowerSaveDigitDarkLevel = level;
        }
        if (Util.showCtsSpecifiedColor()) {
            this.mGraphicPowerSaveDigitIconDarkMode.setColorFilter(this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts), PorterDuff.Mode.SRC_IN);
        } else {
            this.mGraphicPowerSaveDigitIconDarkMode.setColorFilter(null);
        }
        return this.mGraphicPowerSaveDigitIconDarkMode;
    }

    private LevelListDrawable generateIcon(int resId, int level, boolean plugged) {
        LevelListDrawable result = new LevelListDrawable();
        ArrayList<Drawable> drawables = extractDrawable(resId);
        int count = drawables.size();
        if (count > 0) {
            float sum = 0.4f;
            float delta = 100.0f / ((float) count);
            int start = plugged ? level : level + -10 < 0 ? 0 : level - 10;
            int end = 100;
            if (!plugged) {
                end = level;
            } else if (level + 10 <= 100) {
                end = level + 10;
            }
            for (int i = 0; i < count; i++) {
                int low = (int) sum;
                int high = (int) (sum + delta);
                if (high < start || low > end) {
                    result.addLevel(low, high, null);
                } else {
                    result.addLevel(low, high, drawables.get(i));
                }
                sum += delta;
            }
        }
        result.setAutoMirrored(true);
        return result;
    }

    private ArrayList<Drawable> extractDrawable(int resId) {
        ArrayList<Drawable> result = this.mGraphicRes2Drawables.get(resId, null);
        if (result != null) {
            return result;
        }
        ArrayList<Drawable> result2 = doExtractDrawable(resId);
        this.mGraphicRes2Drawables.put(resId, result2);
        return result2;
    }

    private ArrayList<Drawable> doExtractDrawable(int resId) {
        int iconUnit;
        ArrayList<Drawable> result = new ArrayList<>();
        Resources res = this.mContext.getResources();
        TypedValue value = new TypedValue();
        InputStream is = res.openRawResource(resId, value);
        Bitmap bm = BitmapFactory.decodeStream(is);
        try {
            is.close();
        } catch (IOException e) {
            IOException iOException = e;
            e.printStackTrace();
        }
        if (bm == null) {
            return result;
        }
        int density = Math.max(value.density, 240);
        if (density == 240) {
            iconUnit = 38;
        } else if (density == 320) {
            iconUnit = 50;
        } else if (density == 640) {
            iconUnit = 72;
        } else {
            iconUnit = 60;
        }
        int iconWidth = bm.getWidth() / this.mBatteryColumns;
        int rowCount = bm.getHeight() / iconUnit;
        int columnCount = bm.getWidth() / iconWidth;
        int[] pixels = new int[(iconUnit * iconWidth)];
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < rowCount) {
                int j = 0;
                while (true) {
                    int j2 = j;
                    if (j2 >= columnCount) {
                        break;
                    }
                    int columnCount2 = i2 * iconUnit;
                    int i3 = i2;
                    int i4 = iconWidth;
                    int[] pixels2 = pixels;
                    int columnCount3 = columnCount;
                    int density2 = density;
                    bm.getPixels(pixels, 0, i4, j2 * iconWidth, columnCount2, iconWidth, iconUnit);
                    Bitmap icon = Bitmap.createBitmap(pixels2, 0, iconWidth, i4, iconUnit, Bitmap.Config.ARGB_8888);
                    icon.setDensity(density2);
                    result.add(new BitmapDrawable(res, icon));
                    j = j2 + 1;
                    density = density2;
                    i2 = i3;
                    columnCount = columnCount3;
                    rowCount = rowCount;
                    pixels = pixels2;
                }
                int[] iArr = pixels;
                int i5 = columnCount;
                int i6 = rowCount;
                int i7 = density;
                i = i2 + 1;
            } else {
                int i8 = columnCount;
                int i9 = rowCount;
                int i10 = density;
                bm.recycle();
                return result;
            }
        }
    }

    public void clear() {
        this.mGraphicIcon = null;
        this.mGraphicIconDarkMode = null;
        this.mGraphicDigitalIcon = null;
        this.mGraphicDigitalIconDarkMode = null;
        this.mGraphicChargeIcon = null;
        this.mGraphicChargeIconDarkMode = null;
        this.mGraphicChargeDigitIcon = null;
        this.mGraphicChargeDigitIconDarkMode = null;
        this.mGraphicPowerSaveIcon = null;
        this.mGraphicPowerSaveIconDarkMode = null;
        this.mGraphicPowerSaveDigitIcon = null;
        this.mGraphicPowerSaveDigitIconDarkMode = null;
        this.mGraphicRes2Drawables.clear();
        this.mLevel = -1;
        this.mDarkLevel = -1;
        this.mDigitalLevel = -1;
        this.mDigitalDarkLevel = -1;
        this.mChargeLevel = -1;
        this.mChargeDarkLevel = -1;
        this.mChargeDigitLevel = -1;
        this.mChargeDigitDarkLevel = -1;
        this.mPowerSaveLevel = -1;
        this.mPowerSaveDarkLevel = -1;
        this.mPowerSaveDigitLevel = -1;
        this.mPowerSaveDigitDarkLevel = -1;
    }
}
