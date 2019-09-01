package com.android.settingslib.display;

import android.os.PowerManager;
import android.util.MathUtils;

public class BrightnessUtils {
    public static final int GAMMA_SPACE_MAX = PowerManager.BRIGHTNESS_ON;

    public static final int convertGammaToLinear(int val, int min, int max) {
        float ret;
        float normalizedVal = MathUtils.norm(0.0f, (float) GAMMA_SPACE_MAX, (float) val);
        if (normalizedVal <= 0.2f) {
            ret = MathUtils.sq(normalizedVal / 0.2f);
        } else {
            ret = MathUtils.exp((normalizedVal - 0.221f) / 0.314f) + 0.06f;
        }
        int tmpVal = Math.round(MathUtils.lerp((float) min, (float) max, ret / 12.0f));
        return tmpVal > max ? max : tmpVal;
    }

    public static final int convertLinearToGamma(int val, int min, int max) {
        float ret;
        float normalizedVal = MathUtils.norm((float) min, (float) max, (float) val) * 12.0f;
        if (normalizedVal <= 1.0f) {
            ret = MathUtils.sqrt(normalizedVal) * 0.2f;
        } else {
            ret = (0.314f * MathUtils.log(normalizedVal - 0.06f)) + 0.221f;
        }
        return Math.round(MathUtils.lerp(0.0f, (float) GAMMA_SPACE_MAX, ret));
    }
}
