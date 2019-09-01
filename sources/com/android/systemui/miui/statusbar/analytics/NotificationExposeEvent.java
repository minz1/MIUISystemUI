package com.android.systemui.miui.statusbar.analytics;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationExposeEvent implements INotificationEvent {
    private final String MESSAGE_LIST = "messageList";
    private List<ExposeMessage> mMessageList;
    private NotificationEvent mNotificationEvent;

    public NotificationExposeEvent(NotificationEvent notificationEvent, List<ExposeMessage> messageList) {
        this.mNotificationEvent = notificationEvent;
        this.mMessageList = new ArrayList(messageList);
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            jsonObject = this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        if (this.mMessageList != null) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < this.mMessageList.size(); i++) {
                jsonArray.put(this.mMessageList.get(i).toJSONObject().toString());
            }
            try {
                jsonObject.put("messageList", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            jsonObject.put("event", "expose");
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "expose", jsonObject.toString());
    }
}
