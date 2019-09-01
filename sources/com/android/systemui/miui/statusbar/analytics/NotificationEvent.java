package com.android.systemui.miui.statusbar.analytics;

import android.service.notification.StatusBarNotification;
import com.android.systemui.miui.statusbar.notification.FoldBucketHelper;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationEvent {
    private long mCreateTimeStamp;
    private boolean mFold;
    private int mImportance;
    private double mLocalScore;
    private boolean mNewly;
    private boolean mNoScore;
    private NotificationContent mNotificationContent;
    private NotificationEnv mNotificationEnv = new NotificationEnv();
    private String mPackageName;
    private double mPushScore;
    private int mVersion;

    public NotificationEvent(StatusBarNotification sbn, String packageName) {
        this.mNotificationContent = new NotificationContent(sbn);
        this.mPackageName = packageName;
    }

    public void setImportance(int importance) {
        this.mImportance = importance;
    }

    public void setPushScore(double pushScore) {
        this.mPushScore = pushScore;
    }

    public void setLocalScore(double localScore) {
        this.mLocalScore = localScore;
    }

    public void setNoScore() {
        this.mNoScore = true;
    }

    public void setNewly(boolean newly) {
        this.mNewly = newly;
    }

    public void setFold(boolean fold) {
        this.mFold = fold;
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mNotificationEnv.setStatusBar(statusBar);
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mNotificationContent.setGroupManager(groupManager);
    }

    public long getCreateTimeStamp() {
        return this.mCreateTimeStamp;
    }

    public void setCreateTimeStamp(long timeStamp) {
        this.mCreateTimeStamp = timeStamp;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        JSONObject jsonObject2 = this.mNotificationContent.wrapJSONObject(jsonObject);
        this.mNotificationEnv.init();
        this.mNotificationEnv.wrapJSONObject(jsonObject2);
        try {
            jsonObject2.put("create_timestamp", this.mCreateTimeStamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            jsonObject2.put("package", this.mPackageName);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        try {
            jsonObject2.put("bucket", FoldBucketHelper.getFoldBucket());
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        try {
            jsonObject2.put("no_score", this.mNoScore);
        } catch (JSONException e4) {
            e4.printStackTrace();
        }
        try {
            jsonObject2.put("push_score", this.mPushScore);
        } catch (JSONException e5) {
            e5.printStackTrace();
        }
        try {
            jsonObject2.put("local_score", this.mLocalScore);
        } catch (JSONException e6) {
            e6.printStackTrace();
        }
        try {
            jsonObject2.put("importance", this.mImportance);
        } catch (JSONException e7) {
            e7.printStackTrace();
        }
        try {
            jsonObject2.put("newly", this.mNewly);
        } catch (JSONException e8) {
            e8.printStackTrace();
        }
        try {
            jsonObject2.put("fold", this.mFold);
        } catch (JSONException e9) {
            e9.printStackTrace();
        }
        try {
            jsonObject2.put("user_fold", NotificationUtil.isUserFold());
        } catch (JSONException e10) {
            e10.printStackTrace();
        }
        try {
            jsonObject2.put("user_fold_lines_count", NotificationUtil.getUserFoldLinesCount());
        } catch (JSONException e11) {
            e11.printStackTrace();
        }
        try {
            jsonObject2.put("version", this.mVersion);
        } catch (JSONException e12) {
            e12.printStackTrace();
        }
        return jsonObject2;
    }
}
