package com.android.keyguard.widget;

public class AODBg {
    private static Integer[] sClockBgHigh = {null, null, null, null, null, null, null, Integer.valueOf(R.drawable.aod_bg_paint), Integer.valueOf(R.drawable.aod_bg_shadow), Integer.valueOf(R.drawable.aod_bg_moonlight), Integer.valueOf(R.drawable.aod_bg_tree_high), Integer.valueOf(R.drawable.aod_bg_cactus_high), Integer.valueOf(R.drawable.aod_bg_succulent_high), Integer.valueOf(R.drawable.aod_bg_spirit_high), Integer.valueOf(R.drawable.aod_bg_ghost), Integer.valueOf(R.drawable.aod_bg_spaceman)};
    public static int[] sIconMask = {0, 0, AODSettings.ICON_PINK_X, AODSettings.ICON_BLUE_X, 0, AODSettings.ICON_PINK_Y, AODSettings.ICON_BLUE_Y, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static Integer[] getClockBg() {
        return sClockBgHigh;
    }
}
