package com.android.systemui.miui.volume;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Util {
    public static boolean DEBUG = Log.isLoggable("volume", 3);

    public static String logTag(Class<?> c) {
        String tag = "vol." + c.getSimpleName();
        return tag.length() < 23 ? tag : tag.substring(0, 23);
    }

    public static final void setVisOrGone(View v, boolean vis) {
        if (v != null) {
            int i = 8;
            if (v.getVisibility() != (vis ? 0 : 8)) {
                if (vis) {
                    i = 0;
                }
                v.setVisibility(i);
            }
        }
    }

    public static final void setVisOrInvis(View v, boolean vis) {
        if (v != null) {
            int i = 4;
            if (v.getVisibility() != (vis ? 0 : 4)) {
                if (vis) {
                    i = 0;
                }
                v.setVisibility(i);
            }
        }
    }

    public static final void setVisOrInvis(Drawable d, boolean vis) {
        d.setAlpha(vis ? 255 : 0);
    }

    public static int constrain(int amount, int low, int high) {
        if (amount < low) {
            return low;
        }
        return amount > high ? high : amount;
    }

    public static float constrain(float amount, float low, float high) {
        if (amount < low) {
            return low;
        }
        return amount > high ? high : amount;
    }

    public static void reparentChildren(ViewGroup from, ViewGroup to) {
        List<View> views = new ArrayList<>();
        Map<View, ViewGroup.LayoutParams> params = new HashMap<>();
        for (int i = 0; i < from.getChildCount(); i++) {
            View view = from.getChildAt(i);
            views.add(view);
            params.put(view, view.getLayoutParams());
        }
        for (View view2 : views) {
            ((ViewGroup) view2.getParent()).removeView(view2);
            to.addView(view2, params.get(view2));
        }
    }

    public static void setLastTotalCountDownTime(Context context, int time) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("miui_last_count_down_time", time).apply();
    }

    public static int getLastTotalCountDownTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("miui_last_count_down_time", 0);
    }
}
