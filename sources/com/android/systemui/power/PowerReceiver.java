package com.android.systemui.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PowerReceiver extends BroadcastReceiver {
    public static String ACTION_OPEN_SAVE_MODE = "com.android.systemui.OPEN_SAVE_MODE";

    public void onReceive(Context context, Intent intent) {
        if (ACTION_OPEN_SAVE_MODE.equals(intent.getAction())) {
            PowerUtils.hideLowBatteryNotification(context);
            PowerUtils.enableSaveMode(context);
        }
    }
}
