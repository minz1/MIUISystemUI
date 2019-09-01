package com.android.systemui.miui.statusbar.analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class ActionClickEvent implements INotificationEvent {
    protected final String ACTION_INDEX = "action_index";
    protected final String CLICK_TIMESTAMP = "click_timestamp";
    private int mActionIndex;
    private long mClickTimestamp;
    private NotificationEvent mNotificationEvent;

    public ActionClickEvent(NotificationEvent notificationEvent, int actionIndex) {
        this.mNotificationEvent = notificationEvent;
        this.mClickTimestamp = System.currentTimeMillis();
        this.mActionIndex = actionIndex;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "action_click", jsonObject.toString());
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        if (this.mClickTimestamp > 0) {
            try {
                jsonObject.put("click_timestamp", this.mClickTimestamp);
            } catch (JSONException e) {
            }
        }
        try {
            jsonObject.put("action_index", this.mActionIndex);
        } catch (JSONException e2) {
        }
        try {
            jsonObject.put("event", "action_click");
        } catch (JSONException e3) {
        }
        return jsonObject;
    }
}
