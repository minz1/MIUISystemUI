package com.android.systemui.screenshot;

import android.content.Context;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;

public class StatHelper {
    public static void recordCountEvent(Context context, String event) {
        recordCountEvent(context, event, (Map<String, String>) null);
    }

    public static void recordCountEvent(Context context, String event, String subCategoryValue) {
        HashMap<String, String> params = new HashMap<>();
        params.put("category", subCategoryValue);
        recordCountEvent(context, event, (Map<String, String>) params);
    }

    public static void recordCountEvent(Context context, String event, Map<String, String> params) {
        Intent intent = new Intent("com.miui.gallery.intent.action.SEND_STAT");
        intent.setPackage("com.miui.gallery");
        intent.putExtra("stat_type", "count_event");
        intent.putExtra("category", "screenshot");
        intent.putExtra("event", event);
        if (params != null && params.size() > 0) {
            intent.putExtra("param_keys", (String[]) params.keySet().toArray(new String[0]));
            intent.putExtra("param_values", (String[]) params.values().toArray(new String[0]));
        }
        context.sendBroadcast(intent);
    }

    public static void recordNumericPropertyEvent(Context context, String event, long value) {
        Intent intent = new Intent("com.miui.gallery.intent.action.SEND_STAT");
        intent.setPackage("com.miui.gallery");
        intent.putExtra("stat_type", "numeric_event");
        intent.putExtra("category", "screenshot");
        intent.putExtra("event", event);
        intent.putExtra("value", value);
        context.sendBroadcast(intent);
    }
}
