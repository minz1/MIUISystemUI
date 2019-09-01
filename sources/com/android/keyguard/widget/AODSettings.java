package com.android.keyguard.widget;

import android.os.Build;

public class AODSettings {
    public static final String AOD_MODE = (Build.VERSION.SDK_INT >= 28 ? "doze_always_on" : "aod_mode");
    public static final int ICON_BLUE_X = R.drawable.icon_blue_x;
    public static final int ICON_BLUE_Y = R.drawable.icon_blue_y;
    public static final int ICON_PINK_X = R.drawable.icon_pink_x;
    public static final int ICON_PINK_Y = R.drawable.icon_pink_y;
    private static int[] sClockColor = {0, 2, 1, 0, 0, 2, 1, 0, 0, 0, 0, 0};
    private static int[] sClockColorHigh = {0, 0, 2, 1, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] sClockOrientation = {0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2};
    private static final int[] sClockOrientationHigh = {5, 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2};

    public static boolean isHighPerformace() {
        return miui.os.Build.DEVICE.equals("cepheus") || miui.os.Build.DEVICE.equals("grus") || miui.os.Build.DEVICE.equals("davinci") || miui.os.Build.DEVICE.equals("raphael") || miui.os.Build.DEVICE.equals("davinciin") || miui.os.Build.DEVICE.equals("raphaelin");
    }

    public static boolean supportColorImage() {
        return miui.os.Build.DEVICE.equals("cepheus") || miui.os.Build.DEVICE.equals("grus") || miui.os.Build.DEVICE.equals("davinci") || miui.os.Build.DEVICE.equals("raphael") || miui.os.Build.DEVICE.equals("davinciin") || miui.os.Build.DEVICE.equals("raphaelin") || miui.os.Build.DEVICE.equals("perseus");
    }

    public static int[] getClockColor() {
        return isHighPerformace() ? sClockColorHigh : sClockColor;
    }

    public static int[] getClockOrientation() {
        return isHighPerformace() ? sClockOrientationHigh : sClockOrientation;
    }

    public static int getIconMask(int index) {
        if (supportColorImage()) {
            return AODBg.sIconMask[index];
        }
        return 0;
    }
}
