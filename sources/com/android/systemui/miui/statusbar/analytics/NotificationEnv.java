package com.android.systemui.miui.statusbar.analytics;

import android.content.Context;
import android.media.AudioManager;
import com.android.systemui.statusbar.phone.StatusBar;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationEnv {
    private int mBatteryLevel;
    private boolean mMediaActive;
    private StatusBar mStatusBar;

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    public void init() {
        this.mMediaActive = isMediaActive(this.mStatusBar.mContext);
        this.mBatteryLevel = getBatteryLevel(this.mStatusBar);
    }

    public static boolean isMediaActive(Context context) {
        return ((AudioManager) context.getSystemService("audio")).isMusicActive();
    }

    public static int getBatteryLevel(StatusBar phoneStatusBar) {
        return phoneStatusBar.getBatteryLevel();
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        try {
            jsonObject.put("media_active", this.mMediaActive);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            jsonObject.put("battery_level", this.mBatteryLevel);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return jsonObject;
    }
}
