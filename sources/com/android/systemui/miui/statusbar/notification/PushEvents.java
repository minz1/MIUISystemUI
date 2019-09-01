package com.android.systemui.miui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.miui.statusbar.LocalAlgoModel;
import com.google.gson.Gson;

public class PushEvents {
    public static String getMessageId(Notification notification) {
        if (notification != null) {
            Bundle extras = notification.extras;
            if (extras != null) {
                String messageId = extras.getString("message_id");
                if (!TextUtils.isEmpty(messageId)) {
                    return messageId;
                }
            }
        }
        return null;
    }

    public static String getEventMessageType(Notification notification) {
        if (notification != null) {
            Bundle extras = notification.extras;
            if (extras != null) {
                String eventMessageType = extras.getString("eventMessageType");
                if (!TextUtils.isEmpty(eventMessageType)) {
                    return eventMessageType;
                }
            }
        }
        return null;
    }

    public static String getADId(Notification notification) {
        if (notification != null) {
            Bundle extras = notification.extras;
            if (extras != null) {
                long ad_id = extras.getLong("adid");
                if (ad_id != 0) {
                    return ad_id + "";
                }
            }
        }
        return null;
    }

    public static ScoreInfo getScoreInfo(Notification notification) {
        if (notification != null) {
            Bundle extras = notification.extras;
            if (extras != null) {
                String rawText = extras.getString("score_info");
                if (!TextUtils.isEmpty(rawText)) {
                    return (ScoreInfo) new Gson().fromJson(rawText, ScoreInfo.class);
                }
            }
        }
        return null;
    }

    public static void persistLocalModel(Context context, ScoreInfo scoreInfo) {
        if (scoreInfo != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String localModel = scoreInfo.toJSONObject();
            if (!TextUtils.isEmpty(localModel)) {
                sp.edit().putString("local_model", localModel).commit();
            }
        }
    }

    public static void restoreLocalModel(Context context) {
        String localModel = getLocalModelStr(context);
        if (!TextUtils.isEmpty(localModel)) {
            ScoreInfo scoreInfo = (ScoreInfo) new Gson().fromJson(localModel, ScoreInfo.class);
            Log.v("PushEvents", "restore" + scoreInfo.toJSONObject());
            LocalAlgoModel.restoreUpdateTime(context);
            LocalAlgoModel.updateLocalModel(context, scoreInfo);
        }
    }

    public static String getLocalModelStr(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.contains("local_model")) {
            return sp.getString("local_model", null);
        }
        return null;
    }
}
