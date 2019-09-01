package com.android.systemui;

import android.view.View;
import android.widget.TextView;

public class FontUtils {
    public static void updateFontColor(View parent, int viewId, int colorId) {
        View view = parent.findViewById(viewId);
        if (view != null && (view instanceof TextView)) {
            updateFontColor((TextView) view, colorId);
        }
    }

    public static void updateFontSize(View parent, int viewId, int dimensId) {
        View view = parent.findViewById(viewId);
        if (view != null && (view instanceof TextView)) {
            updateFontSize((TextView) view, dimensId);
        }
    }

    public static void updateFontColor(TextView v, int colorId) {
        v.setTextColor(v.getContext().getColor(colorId));
    }

    public static void updateFontSize(TextView v, int dimensId) {
        v.setTextSize(0, (float) v.getResources().getDimensionPixelSize(dimensId));
    }
}
