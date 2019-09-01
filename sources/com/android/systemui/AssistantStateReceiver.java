package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.internal.app.AssistUtils;

public class AssistantStateReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        boolean enabled = intent.getBooleanExtra("OPA_ENABLED", false);
        Log.i("AssistantStateReceiver", "Received " + intent + " with enabled = " + enabled);
        UserSettingsUtils.save(context.getContentResolver(), enabled);
        new OpaEnableDispatcher(context, new AssistUtils(context)).dispatchOpaEnabled(enabled);
    }
}
