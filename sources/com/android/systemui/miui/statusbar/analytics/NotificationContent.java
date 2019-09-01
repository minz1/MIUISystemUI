package com.android.systemui.miui.statusbar.analytics;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.service.notification.StatusBarNotificationCompat;
import android.text.TextUtils;
import com.android.systemui.Constants;
import com.android.systemui.miui.statusbar.notification.PushEvents;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationContent {
    private static boolean DEBUG = Constants.DEBUG;
    private String mEventMessageType;
    private Bundle mExtras = this.mNotification.extras;
    private NotificationGroupManager mGroupManager;
    private boolean mHasActions;
    private boolean mHasCustomView;
    private boolean mHasLargeIcon;
    private String mId;
    private boolean mIsClearable;
    private Notification mNotification;
    private int mPriority;
    private String mPushId;
    private StatusBarNotification mSbn;
    private String mStyle;

    public NotificationContent(StatusBarNotification sbn) {
        this.mSbn = sbn;
        this.mNotification = sbn.getNotification();
        this.mId = NotificationUtils.generateNotificationId(sbn);
        boolean z = false;
        this.mHasLargeIcon = this.mExtras.get("android.largeIcon") != null;
        this.mHasCustomView = this.mExtras.get("android.contains.customView") != null;
        if (this.mNotification.actions != null && this.mNotification.actions.length > 0) {
            z = true;
        }
        this.mHasActions = z;
        this.mIsClearable = isClearable();
        this.mPriority = this.mNotification.priority;
        this.mPushId = PushEvents.getMessageId(this.mNotification);
        this.mEventMessageType = PushEvents.getEventMessageType(this.mNotification);
        this.mStyle = this.mExtras.getString("android.template");
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    private boolean isClearable() {
        return (this.mNotification.flags & 2) == 0 && (this.mNotification.flags & 32) == 0;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (!TextUtils.isEmpty(this.mId)) {
            try {
                jsonObject.put("id", this.mId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            jsonObject.put("largeicon", this.mHasLargeIcon);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        try {
            jsonObject.put("custom_icon", this.mHasCustomView);
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        try {
            jsonObject.put("custom_action", this.mHasActions);
        } catch (JSONException e4) {
            e4.printStackTrace();
        }
        try {
            jsonObject.put("clearable", this.mIsClearable);
        } catch (JSONException e5) {
            e5.printStackTrace();
        }
        try {
            jsonObject.put("priority", this.mPriority);
        } catch (JSONException e6) {
            e6.printStackTrace();
        }
        if (!TextUtils.isEmpty(this.mPushId)) {
            try {
                jsonObject.put("push_id", this.mPushId);
            } catch (JSONException e7) {
                e7.printStackTrace();
            }
        }
        if (!TextUtils.isEmpty(this.mEventMessageType)) {
            try {
                jsonObject.put("event_message_type", this.mEventMessageType);
            } catch (JSONException e8) {
                e8.printStackTrace();
            }
        }
        if (!TextUtils.isEmpty(this.mStyle)) {
            try {
                jsonObject.put("style", this.mStyle);
            } catch (JSONException e9) {
                e9.printStackTrace();
            }
        }
        try {
            jsonObject.put("notification_type", calculateNotificationType());
        } catch (JSONException e10) {
            e10.printStackTrace();
        }
        try {
            jsonObject.put("group_key", getGroupKey());
        } catch (JSONException e11) {
            e11.printStackTrace();
        }
        if (DEBUG) {
            try {
                jsonObject.put("title_debug", this.mExtras.getCharSequence("android.title"));
            } catch (JSONException e12) {
                e12.printStackTrace();
            }
        }
        return jsonObject;
    }

    private int calculateNotificationType() {
        if (this.mGroupManager.isSummaryOfGroup(this.mSbn)) {
            return 1;
        }
        return this.mGroupManager.isChildInGroupWithSummary(this.mSbn) ? 2 : 0;
    }

    private String getGroupKey() {
        String groupKey = this.mNotification.getGroup();
        if (this.mGroupManager.isSummaryOfGroup(this.mSbn)) {
            if (TextUtils.equals(groupKey, Constants.AUTOGROUP_KEY)) {
                return "com.android.systemui";
            }
            return groupKey;
        } else if (!this.mGroupManager.isChildInGroupWithSummary(this.mSbn)) {
            return "";
        } else {
            if (StatusBarNotificationCompat.isAppGroup(this.mSbn)) {
                return groupKey;
            }
            return "com.android.systemui";
        }
    }
}
