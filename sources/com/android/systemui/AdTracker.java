package com.android.systemui;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.statusbar.NotificationData;
import com.xiaomi.analytics.Actions;
import com.xiaomi.analytics.AdAction;
import com.xiaomi.analytics.Analytics;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdTracker {
    public static void trackShow(Context context, NotificationData.Entry entry) {
        JSONObject json = getJSONTag(entry);
        trackEvent(context, "VIEW", getExtra(json), entry, gerMonitorUrl(json, "viewMonitorUrls"));
    }

    public static void trackRemove(Context context, NotificationData.Entry entry) {
        trackEvent(context, "NOTIFICATION_REMOVE", getExtra(getJSONTag(entry)), entry, null);
    }

    private static void trackEvent(Context context, String event, String extra, NotificationData.Entry entry, List<String> urls) {
        if (!TextUtils.isEmpty(extra)) {
            AdAction adAction = Actions.newAdAction(event);
            adAction.addParam("ex", extra);
            adAction.addParam("v", "sdk_1.0");
            adAction.addParam("e", event);
            adAction.addParam("t", System.currentTimeMillis());
            if (urls != null) {
                adAction.addAdMonitor(urls);
            }
            try {
                String configKey = Constants.DEBUG ? "systemui_pushstaging" : "systemui_push";
                Log.d("adTracker", "config = " + configKey);
                Analytics.trackSystem(context, configKey, adAction);
            } catch (Exception e) {
                Log.e("adTracker", e.getLocalizedMessage());
            }
        }
    }

    public static String getExtra(JSONObject jsonTag) {
        if (jsonTag != null) {
            return jsonTag.optString("ex");
        }
        return null;
    }

    public static List<String> gerMonitorUrl(JSONObject jsonTag, String monitor) {
        if (jsonTag != null) {
            return JSONArrayToList(jsonTag.optJSONArray(monitor));
        }
        return null;
    }

    public static JSONObject getJSONTag(NotificationData.Entry entry) {
        String tag = entry.notification.getTag();
        if (!TextUtils.isEmpty(tag)) {
            try {
                return new JSONObject(tag);
            } catch (JSONException e) {
                Log.e("adTracker", e.getLocalizedMessage());
            }
        }
        return null;
    }

    public static List<String> JSONArrayToList(JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }
        List<String> list = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                list.add(jsonArray.getString(i));
            } catch (JSONException e) {
                Log.e("adTracker", e.getLocalizedMessage());
            }
        }
        return list;
    }
}
