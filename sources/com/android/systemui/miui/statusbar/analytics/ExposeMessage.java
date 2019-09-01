package com.android.systemui.miui.statusbar.analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class ExposeMessage {
    private final String EXPOSE_TIMESTAMP = "expose_timestamp";
    private final String IS_GROUP_EXPANDED = "is_group_expanded";
    private final String IS_HEADS_UP = "is_heads_up";
    private final String IS_KEYGUARD = "is_keyguard";
    private final String LENGTH = "length";
    private long mExposeTimestamp;
    private boolean mIsGroupExpanded;
    private boolean mIsHeadsUp;
    private boolean mIsKeyguard;
    private long mLength;

    public ExposeMessage(long exposeTimestamp, boolean isGroupExpanded, boolean isHeadsUp, boolean isKeyguard) {
        this.mExposeTimestamp = exposeTimestamp;
        this.mLength = System.currentTimeMillis() - this.mExposeTimestamp;
        this.mIsGroupExpanded = isGroupExpanded;
        this.mIsHeadsUp = isHeadsUp;
        this.mIsKeyguard = isKeyguard;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        if (this.mLength > 0) {
            if (this.mExposeTimestamp > 0) {
                try {
                    jsonObject.put("expose_timestamp", this.mExposeTimestamp);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            try {
                jsonObject.put("length", this.mLength);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
            try {
                jsonObject.put("is_group_expanded", this.mIsGroupExpanded);
            } catch (JSONException e3) {
                e3.printStackTrace();
            }
            try {
                jsonObject.put("is_heads_up", this.mIsHeadsUp);
            } catch (JSONException e4) {
                e4.printStackTrace();
            }
            try {
                jsonObject.put("is_keyguard", this.mIsKeyguard);
            } catch (JSONException e5) {
                e5.printStackTrace();
            }
        }
        return jsonObject;
    }
}
