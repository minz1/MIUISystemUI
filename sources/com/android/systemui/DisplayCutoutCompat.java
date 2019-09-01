package com.android.systemui;

import android.graphics.Rect;
import android.graphics.Region;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.View;
import com.android.systemui.statusbar.phone.StatusBar;

public class DisplayCutoutCompat {
    public static int getSafeInsetLeft(StatusBar statusBar, DisplayInfo info) {
        DisplayCutout displayCutout = info.displayCutout;
        if (displayCutout == null) {
            return 0;
        }
        return displayCutout.getSafeInsetLeft();
    }

    public static int getSafeInsetRight(StatusBar statusBar, DisplayInfo info) {
        DisplayCutout displayCutout = info.displayCutout;
        if (displayCutout == null) {
            return 0;
        }
        return displayCutout.getSafeInsetRight();
    }

    public static int getHeight(DisplayInfo info) {
        DisplayCutout displayCutout = info.displayCutout;
        if (displayCutout == null) {
            return 0;
        }
        int height = Integer.MAX_VALUE;
        int inset = displayCutout.getSafeInsetTop();
        if (inset > 0) {
            height = Math.min(Integer.MAX_VALUE, inset);
        }
        int inset2 = displayCutout.getSafeInsetBottom();
        if (inset2 > 0) {
            height = Math.min(height, inset2);
        }
        int inset3 = displayCutout.getSafeInsetLeft();
        if (inset3 > 0) {
            height = Math.min(height, inset3);
        }
        int inset4 = displayCutout.getSafeInsetRight();
        if (inset4 > 0) {
            height = Math.min(height, inset4);
        }
        if (height == Integer.MAX_VALUE) {
            height = 0;
        }
        return height;
    }

    public static void boundsFromDirection(View view, int gravity, Rect out) {
        DisplayCutout cutout = view.getRootWindowInsets().getDisplayCutout();
        if (cutout != null) {
            boundsFromDirection(cutout, gravity, out);
        }
    }

    public static void boundsFromDirection(DisplayCutout displayCutout, int gravity, Rect out) {
        Region bounds = boundsFromDirection(displayCutout, gravity);
        out.set(bounds.getBounds());
        bounds.recycle();
    }

    public static Region boundsFromDirection(DisplayCutout displayCutout, int gravity) {
        Region bounds = displayCutout.getBounds();
        if (gravity == 3) {
            bounds.op(0, 0, displayCutout.getSafeInsetLeft(), Integer.MAX_VALUE, Region.Op.INTERSECT);
        } else if (gravity == 5) {
            bounds.op(displayCutout.getSafeInsetLeft() + 1, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Region.Op.INTERSECT);
        } else if (gravity == 48) {
            bounds.op(0, 0, Integer.MAX_VALUE, displayCutout.getSafeInsetTop(), Region.Op.INTERSECT);
        } else if (gravity == 80) {
            bounds.op(0, displayCutout.getSafeInsetTop() + 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Region.Op.INTERSECT);
        }
        return bounds;
    }

    public static boolean hasCutout(View view) {
        return view.getRootWindowInsets().getDisplayCutout() != null;
    }
}
