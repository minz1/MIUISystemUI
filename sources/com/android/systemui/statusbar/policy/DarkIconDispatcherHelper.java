package com.android.systemui.statusbar.policy;

import android.graphics.Rect;
import android.view.View;

public abstract class DarkIconDispatcherHelper implements DarkIconDispatcher {
    public static int getTint(Rect tintArea, View view, int color) {
        if (isInArea(tintArea, view)) {
            return color;
        }
        return -1;
    }

    public static float getDarkIntensity(Rect tintArea, View view, float intensity) {
        if (isInArea(tintArea, view)) {
            return intensity;
        }
        return 0.0f;
    }

    public static boolean inDarkMode(Rect tintArea, View view, float intensity) {
        return getDarkIntensity(tintArea, view, intensity) > 0.0f;
    }

    public static boolean isInArea(Rect area, View view) {
        boolean z = true;
        if (area.isEmpty()) {
            return true;
        }
        sTmpRect.set(area);
        view.getLocationOnScreen(sTmpInt2);
        int left = sTmpInt2[0];
        int intersectAmount = Math.max(0, Math.min(view.getWidth() + left, area.right) - Math.max(left, area.left));
        boolean coversFullStatusBar = area.top <= 0;
        if (!(2 * intersectAmount > view.getWidth()) || !coversFullStatusBar) {
            z = false;
        }
        return z;
    }
}
