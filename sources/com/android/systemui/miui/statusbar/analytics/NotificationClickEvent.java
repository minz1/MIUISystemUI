package com.android.systemui.miui.statusbar.analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationClickEvent implements INotificationEvent {
    protected final String CLICK_TIMESTAMP = "click_timestamp";
    protected final String FLOAT_NOTIFICATION = "float_notification";
    protected final String KEYGUARD_NOTIFICATION = "keyguard_notification";
    private long mClickTimestamp;
    private boolean mFloatNotification;
    private boolean mKeyguardNotification;
    private NotificationEvent mNotificationEvent;

    public NotificationClickEvent(NotificationEvent notificationEvent, boolean floatNotification, boolean keyguardNotification) {
        this.mNotificationEvent = notificationEvent;
        this.mClickTimestamp = System.currentTimeMillis();
        this.mFloatNotification = floatNotification;
        this.mKeyguardNotification = keyguardNotification;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "click", jsonObject.toString());
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            jsonObject = this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        if (this.mClickTimestamp > 0) {
            try {
                jsonObject.put("click_timestamp", this.mClickTimestamp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            jsonObject.put("float_notification", this.mFloatNotification);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        try {
            jsonObject.put("keyguard_notification", this.mKeyguardNotification);
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        try {
            jsonObject.put("event", "click");
        } catch (JSONException e4) {
            e4.printStackTrace();
        }
        return jsonObject;
    }
}
