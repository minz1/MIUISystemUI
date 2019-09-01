package com.android.systemui.miui.statusbar.analytics;

import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class FoldEvent implements INotificationEvent {
    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        try {
            jsonObject.put("is_fold_tips", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            jsonObject.put("user_fold", NotificationUtil.isUserFold());
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        try {
            jsonObject.put("notification_type", 3);
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        return jsonObject;
    }
}
