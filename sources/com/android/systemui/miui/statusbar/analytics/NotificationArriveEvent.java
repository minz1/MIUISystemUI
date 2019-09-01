package com.android.systemui.miui.statusbar.analytics;

import com.android.systemui.Dependency;
import com.android.systemui.miui.statusbar.phone.rank.PackageScoreCache;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationArriveEvent implements INotificationEvent {
    private long mArriveTimestamp = System.currentTimeMillis();
    private NotificationEvent mNotificationEvent;

    public NotificationArriveEvent(NotificationEvent notificationEvent) {
        this.mNotificationEvent = notificationEvent;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mArriveTimestamp > 0) {
            try {
                jsonObject.put("arrive_timestamp", this.mArriveTimestamp);
            } catch (JSONException e) {
            }
        }
        PackageScoreCache packageScoreCache = (PackageScoreCache) Dependency.get(PackageScoreCache.class);
        try {
            jsonObject.put("total_click_count", packageScoreCache.getTotalClickCount());
        } catch (JSONException e2) {
        }
        try {
            jsonObject.put("total_show_count", packageScoreCache.getTotalShowCount());
        } catch (JSONException e3) {
        }
        if (this.mNotificationEvent != null) {
            try {
                jsonObject.put("package_click_count", packageScoreCache.getTotalClickCount(this.mNotificationEvent.getPackageName()));
            } catch (JSONException e4) {
            }
            try {
                jsonObject.put("package_show_count", packageScoreCache.getTotalShowCount(this.mNotificationEvent.getPackageName()));
            } catch (JSONException e5) {
            }
            this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        try {
            jsonObject.put("event", "arrive");
        } catch (JSONException e6) {
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "arrive", jsonObject.toString());
    }
}
