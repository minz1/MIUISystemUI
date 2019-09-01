package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.HashMap;

public class HandyModeReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        int mode = intent.getIntExtra("handymode", -1);
        long handyModeTime = intent.getLongExtra("handymodetime", 0);
        if (mode == 1 || mode == 2) {
            AnalyticsHelper.track("HandyMode", "handymode_enter", new HashMap());
        } else if (mode == 0) {
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("handymodetime", Long.toString(handyModeTime));
            parameters.put("handymode", Long.toString((long) mode));
            AnalyticsHelper.track("HandyMode", "handymode_time", parameters);
        }
    }
}
