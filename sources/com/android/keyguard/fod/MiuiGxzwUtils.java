package com.android.keyguard.fod;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.keyguard.KeyguardCompatibilityHelperForP;
import com.android.systemui.R;
import miui.os.Build;

public class MiuiGxzwUtils {
    static final int[] AOD_BACK_ANIM_RES = {R.drawable.gxzw_aod_back_anim_1, R.drawable.gxzw_aod_back_anim_2, R.drawable.gxzw_aod_back_anim_3, R.drawable.gxzw_aod_back_anim_4, R.drawable.gxzw_aod_back_anim_5, R.drawable.gxzw_aod_back_anim_6, R.drawable.gxzw_aod_back_anim_19, R.drawable.gxzw_aod_back_anim_20, R.drawable.gxzw_aod_back_anim_21, R.drawable.gxzw_aod_back_anim_22, R.drawable.gxzw_aod_back_anim_23, R.drawable.gxzw_aod_back_anim_24, R.drawable.gxzw_aod_back_anim_25, R.drawable.gxzw_aod_back_anim_26, R.drawable.gxzw_aod_back_anim_27};
    static final int[] AOD_FALSE_ANIM_RES = {R.drawable.gxzw_aod_false_anim_1, R.drawable.gxzw_aod_false_anim_2, R.drawable.gxzw_aod_false_anim_3, R.drawable.gxzw_aod_false_anim_4, R.drawable.gxzw_aod_false_anim_5, R.drawable.gxzw_aod_false_anim_6, R.drawable.gxzw_aod_false_anim_7, R.drawable.gxzw_aod_false_anim_8, R.drawable.gxzw_aod_false_anim_9, R.drawable.gxzw_aod_false_anim_10, R.drawable.gxzw_aod_false_anim_11, R.drawable.gxzw_aod_false_anim_12, R.drawable.gxzw_aod_false_anim_13, R.drawable.gxzw_aod_false_anim_14, R.drawable.gxzw_aod_false_anim_15};
    static final int[] AOD_ICON_ANIM_RES = {R.drawable.gxzw_aod_icon_anim_1, R.drawable.gxzw_aod_icon_anim_2, R.drawable.gxzw_aod_icon_anim_3, R.drawable.gxzw_aod_icon_anim_4, R.drawable.gxzw_aod_icon_anim_5, R.drawable.gxzw_aod_icon_anim_6, R.drawable.gxzw_aod_icon_anim_7, R.drawable.gxzw_aod_icon_anim_8, R.drawable.gxzw_aod_icon_anim_9, R.drawable.gxzw_aod_icon_anim_10, R.drawable.gxzw_aod_icon_anim_11, R.drawable.gxzw_aod_icon_anim_12, R.drawable.gxzw_aod_icon_anim_13, R.drawable.gxzw_aod_icon_anim_14, R.drawable.gxzw_aod_icon_anim_15, R.drawable.gxzw_aod_icon_anim_16, R.drawable.gxzw_aod_icon_anim_17, R.drawable.gxzw_aod_icon_anim_18, R.drawable.gxzw_aod_icon_anim_19, R.drawable.gxzw_aod_icon_anim_20, R.drawable.gxzw_aod_icon_anim_21, R.drawable.gxzw_aod_icon_anim_22, R.drawable.gxzw_aod_icon_anim_23, R.drawable.gxzw_aod_icon_anim_24, R.drawable.gxzw_aod_icon_anim_25, R.drawable.gxzw_aod_icon_anim_26, R.drawable.gxzw_aod_icon_anim_27, R.drawable.gxzw_aod_icon_anim_28, R.drawable.gxzw_aod_icon_anim_29, R.drawable.gxzw_aod_icon_anim_30, R.drawable.gxzw_aod_icon_anim_31, R.drawable.gxzw_aod_icon_anim_32, R.drawable.gxzw_aod_icon_anim_33, R.drawable.gxzw_aod_icon_anim_34, R.drawable.gxzw_aod_icon_anim_35};
    static final int[] AOD_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_aod_recognizing_anim_1, R.drawable.gxzw_aod_recognizing_anim_2, R.drawable.gxzw_aod_recognizing_anim_3, R.drawable.gxzw_aod_recognizing_anim_4, R.drawable.gxzw_aod_recognizing_anim_5, R.drawable.gxzw_aod_recognizing_anim_6, R.drawable.gxzw_aod_recognizing_anim_7, R.drawable.gxzw_aod_recognizing_anim_8, R.drawable.gxzw_aod_recognizing_anim_9, R.drawable.gxzw_aod_recognizing_anim_10, R.drawable.gxzw_aod_recognizing_anim_11, R.drawable.gxzw_aod_recognizing_anim_12, R.drawable.gxzw_aod_recognizing_anim_13, R.drawable.gxzw_aod_recognizing_anim_14, R.drawable.gxzw_aod_recognizing_anim_15, R.drawable.gxzw_aod_recognizing_anim_16, R.drawable.gxzw_aod_recognizing_anim_17, R.drawable.gxzw_aod_recognizing_anim_18, R.drawable.gxzw_aod_recognizing_anim_19, R.drawable.gxzw_aod_recognizing_anim_20, R.drawable.gxzw_aod_recognizing_anim_21, R.drawable.gxzw_aod_recognizing_anim_22, R.drawable.gxzw_aod_recognizing_anim_23, R.drawable.gxzw_aod_recognizing_anim_24, R.drawable.gxzw_aod_recognizing_anim_25, R.drawable.gxzw_aod_recognizing_anim_26, R.drawable.gxzw_aod_recognizing_anim_27, R.drawable.gxzw_aod_recognizing_anim_28, R.drawable.gxzw_aod_recognizing_anim_29, 0};
    static final int[] DEFAULT_AOD_BACK_ANIM_RES = {R.drawable.finger_image_aod};
    static final int[] DEFAULT_LIGHT_BACK_ANIM_RES = {R.drawable.finger_image_light};
    static final int[] DEFAULT_NORMAL_BACK_ANIM_RES = {R.drawable.finger_image_normal};
    public static int GXZW_ICON_HEIGHT = 173;
    public static int GXZW_ICON_WIDTH = 173;
    public static int GXZW_ICON_X = 453;
    public static int GXZW_ICON_Y = 1640;
    static final int[] LIGHT_BACK_ANIM_RES = {R.drawable.gxzw_light_back_anim_1, R.drawable.gxzw_light_back_anim_2, R.drawable.gxzw_light_back_anim_3, R.drawable.gxzw_light_back_anim_4, R.drawable.gxzw_light_back_anim_5, R.drawable.gxzw_light_back_anim_6, R.drawable.gxzw_light_back_anim_19, R.drawable.gxzw_light_back_anim_20, R.drawable.gxzw_light_back_anim_21, R.drawable.gxzw_light_back_anim_22, R.drawable.gxzw_light_back_anim_23, R.drawable.gxzw_light_back_anim_24, R.drawable.gxzw_light_back_anim_25, R.drawable.gxzw_light_back_anim_26, R.drawable.gxzw_light_back_anim_27};
    static final int[] LIGHT_FALSE_ANIM_RES = {R.drawable.gxzw_light_false_anim_1, R.drawable.gxzw_light_false_anim_2, R.drawable.gxzw_light_false_anim_3, R.drawable.gxzw_light_false_anim_4, R.drawable.gxzw_light_false_anim_5, R.drawable.gxzw_light_false_anim_6, R.drawable.gxzw_light_false_anim_7, R.drawable.gxzw_light_false_anim_8, R.drawable.gxzw_light_false_anim_9, R.drawable.gxzw_light_false_anim_10, R.drawable.gxzw_light_false_anim_11, R.drawable.gxzw_light_false_anim_12, R.drawable.gxzw_light_false_anim_13, R.drawable.gxzw_light_false_anim_14, R.drawable.gxzw_light_false_anim_15};
    static final int[] LIGHT_ICON_ANIM_RES = {R.drawable.gxzw_light_icon_anim_1, R.drawable.gxzw_light_icon_anim_2, R.drawable.gxzw_light_icon_anim_3, R.drawable.gxzw_light_icon_anim_4, R.drawable.gxzw_light_icon_anim_5, R.drawable.gxzw_light_icon_anim_6, R.drawable.gxzw_light_icon_anim_7, R.drawable.gxzw_light_icon_anim_8, R.drawable.gxzw_light_icon_anim_9, R.drawable.gxzw_light_icon_anim_10, R.drawable.gxzw_light_icon_anim_11, R.drawable.gxzw_light_icon_anim_12, R.drawable.gxzw_light_icon_anim_13, R.drawable.gxzw_light_icon_anim_14, R.drawable.gxzw_light_icon_anim_15, R.drawable.gxzw_light_icon_anim_16, R.drawable.gxzw_light_icon_anim_17, R.drawable.gxzw_light_icon_anim_18, R.drawable.gxzw_light_icon_anim_19, R.drawable.gxzw_light_icon_anim_20, R.drawable.gxzw_light_icon_anim_21, R.drawable.gxzw_light_icon_anim_22, R.drawable.gxzw_light_icon_anim_23, R.drawable.gxzw_light_icon_anim_24, R.drawable.gxzw_light_icon_anim_25, R.drawable.gxzw_light_icon_anim_26, R.drawable.gxzw_light_icon_anim_27, R.drawable.gxzw_light_icon_anim_28, R.drawable.gxzw_light_icon_anim_29, R.drawable.gxzw_light_icon_anim_30, R.drawable.gxzw_light_icon_anim_31, R.drawable.gxzw_light_icon_anim_32, R.drawable.gxzw_light_icon_anim_33, R.drawable.gxzw_light_icon_anim_34, R.drawable.gxzw_light_icon_anim_35};
    static final int[] LIGHT_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_light_recognizing_anim_1, R.drawable.gxzw_light_recognizing_anim_2, R.drawable.gxzw_light_recognizing_anim_3, R.drawable.gxzw_light_recognizing_anim_4, R.drawable.gxzw_light_recognizing_anim_5, R.drawable.gxzw_light_recognizing_anim_6, R.drawable.gxzw_light_recognizing_anim_7, R.drawable.gxzw_light_recognizing_anim_8, R.drawable.gxzw_light_recognizing_anim_9, R.drawable.gxzw_light_recognizing_anim_10, R.drawable.gxzw_light_recognizing_anim_11, R.drawable.gxzw_light_recognizing_anim_12, R.drawable.gxzw_light_recognizing_anim_13, R.drawable.gxzw_light_recognizing_anim_14, R.drawable.gxzw_light_recognizing_anim_15, R.drawable.gxzw_light_recognizing_anim_16, R.drawable.gxzw_light_recognizing_anim_17, R.drawable.gxzw_light_recognizing_anim_18, R.drawable.gxzw_light_recognizing_anim_19, R.drawable.gxzw_light_recognizing_anim_20, R.drawable.gxzw_light_recognizing_anim_21, R.drawable.gxzw_light_recognizing_anim_22, R.drawable.gxzw_light_recognizing_anim_23, R.drawable.gxzw_light_recognizing_anim_24, R.drawable.gxzw_light_recognizing_anim_25, R.drawable.gxzw_light_recognizing_anim_26, R.drawable.gxzw_light_recognizing_anim_27, R.drawable.gxzw_light_recognizing_anim_28, R.drawable.gxzw_light_recognizing_anim_29, 0};
    static final int[] NORMAL_BACK_ANIM_RES = {R.drawable.gxzw_normal_back_anim_1, R.drawable.gxzw_normal_back_anim_2, R.drawable.gxzw_normal_back_anim_3, R.drawable.gxzw_normal_back_anim_4, R.drawable.gxzw_normal_back_anim_5, R.drawable.gxzw_normal_back_anim_6, R.drawable.gxzw_normal_back_anim_19, R.drawable.gxzw_normal_back_anim_20, R.drawable.gxzw_normal_back_anim_21, R.drawable.gxzw_normal_back_anim_22, R.drawable.gxzw_normal_back_anim_23, R.drawable.gxzw_normal_back_anim_24, R.drawable.gxzw_normal_back_anim_25, R.drawable.gxzw_normal_back_anim_26, R.drawable.gxzw_normal_back_anim_27};
    static final int[] NORMAL_FALSE_ANIM_RES = {R.drawable.gxzw_normal_false_anim_1, R.drawable.gxzw_normal_false_anim_2, R.drawable.gxzw_normal_false_anim_3, R.drawable.gxzw_normal_false_anim_4, R.drawable.gxzw_normal_false_anim_5, R.drawable.gxzw_normal_false_anim_6, R.drawable.gxzw_normal_false_anim_7, R.drawable.gxzw_normal_false_anim_8, R.drawable.gxzw_normal_false_anim_9, R.drawable.gxzw_normal_false_anim_10, R.drawable.gxzw_normal_false_anim_11, R.drawable.gxzw_normal_false_anim_12, R.drawable.gxzw_normal_false_anim_13, R.drawable.gxzw_normal_false_anim_14, R.drawable.gxzw_normal_false_anim_15};
    static final int[] NORMAL_ICON_ANIM_RES = {R.drawable.gxzw_normal_icon_anim_1, R.drawable.gxzw_normal_icon_anim_2, R.drawable.gxzw_normal_icon_anim_3, R.drawable.gxzw_normal_icon_anim_4, R.drawable.gxzw_normal_icon_anim_5, R.drawable.gxzw_normal_icon_anim_6, R.drawable.gxzw_normal_icon_anim_7, R.drawable.gxzw_normal_icon_anim_8, R.drawable.gxzw_normal_icon_anim_9, R.drawable.gxzw_normal_icon_anim_10, R.drawable.gxzw_normal_icon_anim_11, R.drawable.gxzw_normal_icon_anim_12, R.drawable.gxzw_normal_icon_anim_13, R.drawable.gxzw_normal_icon_anim_14, R.drawable.gxzw_normal_icon_anim_15, R.drawable.gxzw_normal_icon_anim_16, R.drawable.gxzw_normal_icon_anim_17, R.drawable.gxzw_normal_icon_anim_18, R.drawable.gxzw_normal_icon_anim_19, R.drawable.gxzw_normal_icon_anim_20, R.drawable.gxzw_normal_icon_anim_21, R.drawable.gxzw_normal_icon_anim_22, R.drawable.gxzw_normal_icon_anim_23, R.drawable.gxzw_normal_icon_anim_24, R.drawable.gxzw_normal_icon_anim_25, R.drawable.gxzw_normal_icon_anim_26, R.drawable.gxzw_normal_icon_anim_27, R.drawable.gxzw_normal_icon_anim_28, R.drawable.gxzw_normal_icon_anim_29, R.drawable.gxzw_normal_icon_anim_30, R.drawable.gxzw_normal_icon_anim_31, R.drawable.gxzw_normal_icon_anim_32, R.drawable.gxzw_normal_icon_anim_33, R.drawable.gxzw_normal_icon_anim_34, R.drawable.gxzw_normal_icon_anim_35};
    static final int[] NORMAL_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_normal_recognizing_anim_1, R.drawable.gxzw_normal_recognizing_anim_2, R.drawable.gxzw_normal_recognizing_anim_3, R.drawable.gxzw_normal_recognizing_anim_4, R.drawable.gxzw_normal_recognizing_anim_5, R.drawable.gxzw_normal_recognizing_anim_6, R.drawable.gxzw_normal_recognizing_anim_7, R.drawable.gxzw_normal_recognizing_anim_8, R.drawable.gxzw_normal_recognizing_anim_9, R.drawable.gxzw_normal_recognizing_anim_10, R.drawable.gxzw_normal_recognizing_anim_11, R.drawable.gxzw_normal_recognizing_anim_12, R.drawable.gxzw_normal_recognizing_anim_13, R.drawable.gxzw_normal_recognizing_anim_14, R.drawable.gxzw_normal_recognizing_anim_15, R.drawable.gxzw_normal_recognizing_anim_16, R.drawable.gxzw_normal_recognizing_anim_17, R.drawable.gxzw_normal_recognizing_anim_18, R.drawable.gxzw_normal_recognizing_anim_19, R.drawable.gxzw_normal_recognizing_anim_20, R.drawable.gxzw_normal_recognizing_anim_21, R.drawable.gxzw_normal_recognizing_anim_22, R.drawable.gxzw_normal_recognizing_anim_23, R.drawable.gxzw_normal_recognizing_anim_24, R.drawable.gxzw_normal_recognizing_anim_25, R.drawable.gxzw_normal_recognizing_anim_26, R.drawable.gxzw_normal_recognizing_anim_27, R.drawable.gxzw_normal_recognizing_anim_28, R.drawable.gxzw_normal_recognizing_anim_29, 0};
    static final int[] POP_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_pop_recognizing_anim_1, R.drawable.gxzw_pop_recognizing_anim_2, R.drawable.gxzw_pop_recognizing_anim_3, R.drawable.gxzw_pop_recognizing_anim_4, R.drawable.gxzw_pop_recognizing_anim_5, R.drawable.gxzw_pop_recognizing_anim_6, R.drawable.gxzw_pop_recognizing_anim_7, R.drawable.gxzw_pop_recognizing_anim_8, R.drawable.gxzw_pop_recognizing_anim_9, R.drawable.gxzw_pop_recognizing_anim_10, R.drawable.gxzw_pop_recognizing_anim_11, R.drawable.gxzw_pop_recognizing_anim_12, R.drawable.gxzw_pop_recognizing_anim_13, R.drawable.gxzw_pop_recognizing_anim_14};
    public static int PRIVATE_FLAG_IS_HBM_OVERLAY;
    static final int[] PULSE_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_pulse_recognizing_anim_1, R.drawable.gxzw_pulse_recognizing_anim_2, R.drawable.gxzw_pulse_recognizing_anim_3, R.drawable.gxzw_pulse_recognizing_anim_4, R.drawable.gxzw_pulse_recognizing_anim_5, R.drawable.gxzw_pulse_recognizing_anim_6, R.drawable.gxzw_pulse_recognizing_anim_7, R.drawable.gxzw_pulse_recognizing_anim_8, R.drawable.gxzw_pulse_recognizing_anim_9, R.drawable.gxzw_pulse_recognizing_anim_10, R.drawable.gxzw_pulse_recognizing_anim_11, R.drawable.gxzw_pulse_recognizing_anim_12, R.drawable.gxzw_pulse_recognizing_anim_13, R.drawable.gxzw_pulse_recognizing_anim_14, R.drawable.gxzw_pulse_recognizing_anim_15, R.drawable.gxzw_pulse_recognizing_anim_16, R.drawable.gxzw_pulse_recognizing_anim_17, R.drawable.gxzw_pulse_recognizing_anim_18, R.drawable.gxzw_pulse_recognizing_anim_19, R.drawable.gxzw_pulse_recognizing_anim_20, R.drawable.gxzw_pulse_recognizing_anim_21, R.drawable.gxzw_pulse_recognizing_anim_22, R.drawable.gxzw_pulse_recognizing_anim_23, R.drawable.gxzw_pulse_recognizing_anim_24, R.drawable.gxzw_pulse_recognizing_anim_25};
    static final int[] PULSE_WHITE_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_pulse_recognizing_anim_white_1, R.drawable.gxzw_pulse_recognizing_anim_white_2, R.drawable.gxzw_pulse_recognizing_anim_white_3, R.drawable.gxzw_pulse_recognizing_anim_white_4, R.drawable.gxzw_pulse_recognizing_anim_white_5, R.drawable.gxzw_pulse_recognizing_anim_white_6, R.drawable.gxzw_pulse_recognizing_anim_white_7, R.drawable.gxzw_pulse_recognizing_anim_white_8, R.drawable.gxzw_pulse_recognizing_anim_white_9, R.drawable.gxzw_pulse_recognizing_anim_white_10, R.drawable.gxzw_pulse_recognizing_anim_white_11, R.drawable.gxzw_pulse_recognizing_anim_white_12, R.drawable.gxzw_pulse_recognizing_anim_white_13, R.drawable.gxzw_pulse_recognizing_anim_white_14, R.drawable.gxzw_pulse_recognizing_anim_white_15, R.drawable.gxzw_pulse_recognizing_anim_white_16, R.drawable.gxzw_pulse_recognizing_anim_white_17, R.drawable.gxzw_pulse_recognizing_anim_white_18, R.drawable.gxzw_pulse_recognizing_anim_white_19, R.drawable.gxzw_pulse_recognizing_anim_white_20, R.drawable.gxzw_pulse_recognizing_anim_white_21, R.drawable.gxzw_pulse_recognizing_anim_white_22, R.drawable.gxzw_pulse_recognizing_anim_white_23, R.drawable.gxzw_pulse_recognizing_anim_white_24, R.drawable.gxzw_pulse_recognizing_anim_white_25};
    static final int[] RHYTHM_RECOGNIZING_ANIM_RES = {R.drawable.gxzw_rhythm_recognizing_anim_1, R.drawable.gxzw_rhythm_recognizing_anim_2, R.drawable.gxzw_rhythm_recognizing_anim_3, R.drawable.gxzw_rhythm_recognizing_anim_4, R.drawable.gxzw_rhythm_recognizing_anim_5, R.drawable.gxzw_rhythm_recognizing_anim_6, R.drawable.gxzw_rhythm_recognizing_anim_7, R.drawable.gxzw_rhythm_recognizing_anim_8, R.drawable.gxzw_rhythm_recognizing_anim_9, R.drawable.gxzw_rhythm_recognizing_anim_10, R.drawable.gxzw_rhythm_recognizing_anim_11, R.drawable.gxzw_rhythm_recognizing_anim_12, R.drawable.gxzw_rhythm_recognizing_anim_13, R.drawable.gxzw_rhythm_recognizing_anim_14, R.drawable.gxzw_rhythm_recognizing_anim_15, R.drawable.gxzw_rhythm_recognizing_anim_16, R.drawable.gxzw_rhythm_recognizing_anim_17, R.drawable.gxzw_rhythm_recognizing_anim_18, R.drawable.gxzw_rhythm_recognizing_anim_19, R.drawable.gxzw_rhythm_recognizing_anim_20, R.drawable.gxzw_rhythm_recognizing_anim_21, R.drawable.gxzw_rhythm_recognizing_anim_22};
    private static final boolean SUPPORT_LOW_BRIGHTNESS_FOD = (!"equuleus".equals(Build.DEVICE) && !"ursa".equals(Build.DEVICE));

    static {
        PRIVATE_FLAG_IS_HBM_OVERLAY = Integer.MIN_VALUE;
        try {
            PRIVATE_FLAG_IS_HBM_OVERLAY = Class.forName("android.view.WindowManager$LayoutParams").getDeclaredField("PRIVATE_FLAG_IS_HBM_OVERLAY").getInt(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (NoSuchFieldException e3) {
            e3.printStackTrace();
        }
    }

    public static void caculateGxzwIconSize(Context context) {
        String xyString = SystemProperties.get("persist.sys.fp.fod.location.X_Y", "");
        if (xyString.isEmpty()) {
            xyString = SystemProperties.get("persist.vendor.sys.fp.fod.location.X_Y", "");
        }
        String whString = SystemProperties.get("persist.sys.fp.fod.size.width_height", "");
        if (whString.isEmpty()) {
            whString = SystemProperties.get("persist.vendor.sys.fp.fod.size.width_height", "");
        }
        if (xyString.isEmpty() || whString.isEmpty()) {
            resetDefaultValue();
            return;
        }
        try {
            GXZW_ICON_X = Integer.parseInt(xyString.split(",")[0]);
            GXZW_ICON_Y = Integer.parseInt(xyString.split(",")[1]);
            GXZW_ICON_WIDTH = Integer.parseInt(whString.split(",")[0]);
            GXZW_ICON_HEIGHT = Integer.parseInt(whString.split(",")[1]);
            GXZW_ICON_Y -= KeyguardCompatibilityHelperForP.caculateCutoutHeightIfNeed(context);
        } catch (Exception e) {
            e.printStackTrace();
            resetDefaultValue();
        }
    }

    public static int getGxzwCircleColor() {
        if ("equuleus".equals(Build.DEVICE) || "ursa".equals(Build.DEVICE)) {
            return -16711681;
        }
        return -16711936;
    }

    public static boolean hasPressureSensor() {
        return "equuleus".equals(Build.DEVICE) || "ursa".equals(Build.DEVICE);
    }

    public static boolean supportLowBrightnessFod() {
        return SUPPORT_LOW_BRIGHTNESS_FOD;
    }

    public static int getAodInitBrightness() {
        return 17;
    }

    public static int getAod2OnBrightness() {
        return -1;
    }

    public static boolean isFodAodShowEnable(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "gxzw_icon_aod_show_enable", 1, 0) == 1;
    }

    private static void resetDefaultValue() {
        GXZW_ICON_X = 453;
        GXZW_ICON_Y = 1640;
        GXZW_ICON_WIDTH = 173;
        GXZW_ICON_HEIGHT = 173;
    }
}
