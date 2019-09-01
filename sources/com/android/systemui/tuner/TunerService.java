package com.android.systemui.tuner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.systemui.Dependency;

public abstract class TunerService {

    public static class ClearReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.action.CLEAR_TUNER".equals(intent.getAction())) {
                ((TunerService) Dependency.get(TunerService.class)).clearAll();
            }
        }
    }

    public interface Tunable {
        void onTuningChanged(String str, String str2);
    }

    public abstract void addTunable(Tunable tunable, String... strArr);

    public abstract void clearAll();

    public abstract int getValue(String str, int i);

    public abstract String getValue(String str);

    public abstract void removeTunable(Tunable tunable);

    public abstract void setValue(String str, int i);
}
