package com.android.systemui.miui;

import android.content.ComponentName;
import android.content.Intent;
import com.android.systemui.statusbar.policy.CallbackController;

public interface ActivityObserver extends CallbackController<ActivityObserverCallback> {

    public static abstract class ActivityObserverCallback {
        public void activityIdle(Intent intent) {
        }

        public void activityResumed(Intent intent) {
        }

        public void activityPaused(Intent intent) {
        }

        public void activityStopped(Intent intent) {
        }

        public void activityDestroyed(Intent intent) {
        }
    }

    ComponentName getTopActivity();
}
