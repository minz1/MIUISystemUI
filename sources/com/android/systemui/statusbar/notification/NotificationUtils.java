package com.android.systemui.statusbar.notification;

import android.graphics.Color;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.ImageView;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.analytics.Md5;

public class NotificationUtils {
    private static final int[] sLocationBase = new int[2];
    private static final int[] sLocationOffset = new int[2];

    public static boolean isGrayscale(ImageView v, NotificationColorUtil colorUtil) {
        Object isGrayscale = v.getTag(R.id.icon_is_grayscale);
        if (isGrayscale != null) {
            return Boolean.TRUE.equals(isGrayscale);
        }
        boolean grayscale = colorUtil.isGrayscaleIcon(v.getDrawable());
        v.setTag(R.id.icon_is_grayscale, Boolean.valueOf(grayscale));
        return grayscale;
    }

    public static float interpolate(float start, float end, float amount) {
        return ((1.0f - amount) * start) + (end * amount);
    }

    public static int interpolateColors(int startColor, int endColor, float amount) {
        return Color.argb((int) interpolate((float) Color.alpha(startColor), (float) Color.alpha(endColor), amount), (int) interpolate((float) Color.red(startColor), (float) Color.red(endColor), amount), (int) interpolate((float) Color.green(startColor), (float) Color.green(endColor), amount), (int) interpolate((float) Color.blue(startColor), (float) Color.blue(endColor), amount));
    }

    public static float getRelativeYOffset(View offsetView, View baseView) {
        baseView.getLocationOnScreen(sLocationBase);
        offsetView.getLocationOnScreen(sLocationOffset);
        return (float) (sLocationOffset[1] - sLocationBase[1]);
    }

    public static String generateNotificationId(StatusBarNotification sbn) {
        return Md5.getMd5Digest(sbn.getKey());
    }
}
