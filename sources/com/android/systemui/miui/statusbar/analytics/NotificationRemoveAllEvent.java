package com.android.systemui.miui.statusbar.analytics;

import com.android.systemui.miui.statusbar.notification.FoldBucketHelper;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationRemoveAllEvent implements INotificationEvent {
    private NotificationEvent mNotificationEvent;
    private String mRemoveLocation;
    private long mRemoveTimestamp = System.currentTimeMillis();

    public NotificationRemoveAllEvent(NotificationEvent notificationEvent, String removeLocation) {
        this.mNotificationEvent = notificationEvent;
        this.mRemoveLocation = removeLocation;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            jsonObject = this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        try {
            jsonObject.put("remove_location", this.mRemoveLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (this.mRemoveTimestamp > 0) {
            try {
                jsonObject.put("remove_timestamp", this.mRemoveTimestamp);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        try {
            jsonObject.put("bucket", FoldBucketHelper.getFoldBucket());
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        try {
            jsonObject.put("user_fold", NotificationUtil.isUserFold());
        } catch (JSONException e4) {
            e4.printStackTrace();
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "remove_all", jsonObject.toString());
    }
}
