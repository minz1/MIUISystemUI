package com.android.systemui.miui.statusbar.analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationImportanceEvent implements INotificationEvent {
    private int mImportance;
    private NotificationEvent mNotificationEvent;

    public NotificationImportanceEvent(NotificationEvent notificationEvent, int importance) {
        this.mNotificationEvent = notificationEvent;
        this.mImportance = importance;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            jsonObject = this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        try {
            jsonObject.put("event", "set_importance");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (this.mImportance != 0) {
            try {
                jsonObject.put("importance", this.mImportance);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "set_importance", jsonObject.toString());
    }
}
