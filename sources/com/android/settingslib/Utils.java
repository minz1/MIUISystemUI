package com.android.settingslib;

import android.content.Context;
import android.content.res.TypedArray;
import com.android.internal.annotations.VisibleForTesting;
import java.text.NumberFormat;

public class Utils {
    @VisibleForTesting
    static final String STORAGE_MANAGER_SHOW_OPT_IN_PROPERTY = "ro.storage_manager.show_opt_in";
    static final int[] WIFI_PIE = {17302794, 17302795, 17302796, 17302797, 17302798};

    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0d);
    }

    public static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

    public static int getDefaultColor(Context context, int resId) {
        return context.getResources().getColorStateList(resId, context.getTheme()).getDefaultColor();
    }

    public static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }
}
