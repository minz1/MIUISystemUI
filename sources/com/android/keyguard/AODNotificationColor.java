package com.android.keyguard;

import com.android.systemui.R;
import java.util.HashMap;
import java.util.Map;

public class AODNotificationColor {
    private static Map<String, ColorItem> sColorMap = new HashMap();
    private static final ColorItem sDefault;

    static class ColorItem {
        public int left;
        public int right;

        public ColorItem(int l, int r) {
            this.left = l;
            this.right = r;
        }
    }

    static {
        ColorItem blue = new ColorItem(R.drawable.blue_left, R.drawable.blue_right);
        ColorItem green = new ColorItem(R.drawable.green_left, R.drawable.green_right);
        ColorItem purple = new ColorItem(R.drawable.purple_left, R.drawable.purple_right);
        new ColorItem(R.drawable.yellow_left, R.drawable.yellow_right);
        sDefault = purple;
        sColorMap.put("com.tencent.mm", green);
        sColorMap.put("com.tencent.mobileqq", blue);
        sColorMap.put("com.whatsapp", green);
        sColorMap.put("com.facebook.orca", blue);
        sColorMap.put("jp.naver.line.android", green);
        sColorMap.put("com.google.android.gm", purple);
        sColorMap.put("com.android.email", purple);
        sColorMap.put("com.google.android.calendar", purple);
        sColorMap.put("com.android.calendar", purple);
        sColorMap.put("com.android.server.telecom", purple);
        sColorMap.put("com.android.mms", purple);
    }

    public static ColorItem getColorItem(String pkg) {
        return sColorMap.get(pkg) == null ? sDefault : sColorMap.get(pkg);
    }
}
