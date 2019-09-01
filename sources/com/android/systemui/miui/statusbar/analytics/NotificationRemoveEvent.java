package com.android.systemui.miui.statusbar.analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationRemoveEvent implements INotificationEvent {
    private NotificationEvent mNotificationEvent;
    private String mRemoveLocation;
    private long mRemoveTimestamp = System.currentTimeMillis();

    public NotificationRemoveEvent(NotificationEvent notificationEvent, String removeLocation) {
        this.mNotificationEvent = notificationEvent;
        this.mRemoveLocation = removeLocation;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            jsonObject = this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        if (this.mRemoveTimestamp > 0) {
            try {
                jsonObject.put("remove_timestamp", this.mRemoveTimestamp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            jsonObject.put("remove_location", this.mRemoveLocation);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        try {
            jsonObject.put("event", "remove");
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        return new TinyData("notification", "remove", wrapJSONObject(new JSONObject()).toString());
    }
}
