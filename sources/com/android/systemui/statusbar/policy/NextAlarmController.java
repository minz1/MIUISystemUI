package com.android.systemui.statusbar.policy;

import com.android.systemui.Dumpable;

public interface NextAlarmController extends Dumpable, CallbackController<NextAlarmChangeCallback> {

    public interface NextAlarmChangeCallback {
        void onNextAlarmChanged(boolean z);
    }
}
