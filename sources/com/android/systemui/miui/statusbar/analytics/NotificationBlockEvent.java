package com.android.systemui.miui.statusbar.analytics;

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationBlockEvent implements INotificationEvent {
    protected final String BLOCK_LOCATION = "block_location";
    protected final String BLOCK_PACKAGE = "block_package";
    protected final String BLOCK_TIMESTAMP = "block_timestamp";
    private String mBlockLocation;
    private String mBlockPackage;
    private long mBlockTimestamp;
    private NotificationEvent mNotificationEvent;

    public NotificationBlockEvent(NotificationEvent notificationEvent, String blockPackage, String blockLocation) {
        this.mNotificationEvent = notificationEvent;
        this.mBlockPackage = blockPackage;
        this.mBlockLocation = blockLocation;
        this.mBlockTimestamp = System.currentTimeMillis();
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (this.mNotificationEvent != null) {
            this.mNotificationEvent.wrapJSONObject(jsonObject);
        }
        if (!TextUtils.isEmpty(this.mBlockPackage)) {
            try {
                jsonObject.put("block_package", this.mBlockPackage);
            } catch (JSONException e) {
            }
        }
        if (this.mBlockTimestamp > 0) {
            try {
                jsonObject.put("block_timestamp", this.mBlockTimestamp);
            } catch (JSONException e2) {
            }
        }
        if (!TextUtils.isEmpty(this.mBlockLocation)) {
            try {
                jsonObject.put("block_location", this.mBlockLocation);
            } catch (JSONException e3) {
            }
        }
        try {
            jsonObject.put("event", "block");
        } catch (JSONException e4) {
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "block", jsonObject.toString());
    }
}
