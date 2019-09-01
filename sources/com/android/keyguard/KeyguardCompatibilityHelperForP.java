package com.android.keyguard;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.WindowManager;
import com.android.internal.widget.LockPatternUtils;

public class KeyguardCompatibilityHelperForP {
    private static int sPreShowTouches = 0;
    private static int sPreShowTouchesUser = -10000;

    public static void sanitizePassword(LockPatternUtils utils) {
    }

    public static void saveShowTouchesState(Context context) {
        sPreShowTouchesUser = KeyguardUpdateMonitor.getCurrentUser();
        sPreShowTouches = Settings.System.getIntForUser(context.getContentResolver(), "show_touches", 0, sPreShowTouchesUser);
        if (sPreShowTouches != 0) {
            Settings.System.putIntForUser(context.getContentResolver(), "show_touches", 0, sPreShowTouchesUser);
        }
    }

    public static void restoreShowTouchesState(Context context) {
        if (sPreShowTouches != 0) {
            Settings.System.putIntForUser(context.getContentResolver(), "show_touches", sPreShowTouches, sPreShowTouchesUser);
            sPreShowTouches = 0;
            sPreShowTouchesUser = -10000;
        }
    }

    public static void setLayoutInDisplayCutoutMode(WindowManager.LayoutParams lp) {
        lp.layoutInDisplayCutoutMode = 1;
    }

    public static int caculateCutoutHeightIfNeed(Context context) {
        int top = 0;
        if (!MiuiSettings.Global.getBoolean(context.getContentResolver(), "force_black_v2")) {
            return 0;
        }
        Display display = ((DisplayManager) context.getSystemService("display")).getDisplay(0);
        Point point = new Point();
        display.getRealSize(point);
        DisplayCutout dc = DisplayCutout.fromResourcesRectApproximation(context.getResources(), point.x, point.y);
        if (dc != null) {
            top = dc.getSafeInsetTop();
        }
        if (top % 2 != 0) {
            top++;
        }
        return top;
    }
}
