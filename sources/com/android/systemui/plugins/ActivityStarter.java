package com.android.systemui.plugins;

import android.app.PendingIntent;
import android.content.Intent;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(version = 1)
public interface ActivityStarter {
    public static final int VERSION = 1;

    public interface Callback {
        void onActivityStarted(int i);
    }

    void collapsePanels();

    void postQSRunnableDismissingKeyguard(Runnable runnable);

    void postStartActivityDismissingKeyguard(PendingIntent pendingIntent);

    void postStartActivityDismissingKeyguard(Intent intent, int i);

    void startActivity(Intent intent, boolean z);

    void startActivity(Intent intent, boolean z, Callback callback);

    void startActivity(Intent intent, boolean z, boolean z2);

    void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent);
}
