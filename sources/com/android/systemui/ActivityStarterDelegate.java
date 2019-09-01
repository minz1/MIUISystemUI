package com.android.systemui;

import android.app.PendingIntent;
import android.content.Intent;
import com.android.systemui.plugins.ActivityStarter;

public class ActivityStarterDelegate implements ActivityStarter {
    private ActivityStarter mActualStarter;

    public void startPendingIntentDismissingKeyguard(PendingIntent intent) {
        if (this.mActualStarter != null) {
            this.mActualStarter.startPendingIntentDismissingKeyguard(intent);
        }
    }

    public void startActivity(Intent intent, boolean dismissShade) {
        if (this.mActualStarter != null) {
            this.mActualStarter.startActivity(intent, dismissShade);
        }
    }

    public void startActivity(Intent intent, boolean onlyProvisioned, boolean dismissShade) {
        if (this.mActualStarter != null) {
            this.mActualStarter.startActivity(intent, onlyProvisioned, dismissShade);
        }
    }

    public void startActivity(Intent intent, boolean dismissShade, ActivityStarter.Callback callback) {
        if (this.mActualStarter != null) {
            this.mActualStarter.startActivity(intent, dismissShade, callback);
        }
    }

    public void postStartActivityDismissingKeyguard(Intent intent, int delay) {
        if (this.mActualStarter != null) {
            this.mActualStarter.postStartActivityDismissingKeyguard(intent, delay);
        }
    }

    public void postStartActivityDismissingKeyguard(PendingIntent intent) {
        if (this.mActualStarter != null) {
            this.mActualStarter.postStartActivityDismissingKeyguard(intent);
        }
    }

    public void postQSRunnableDismissingKeyguard(Runnable runnable) {
        if (this.mActualStarter != null) {
            this.mActualStarter.postQSRunnableDismissingKeyguard(runnable);
        }
    }

    public void collapsePanels() {
        if (this.mActualStarter != null) {
            this.mActualStarter.collapsePanels();
        }
    }

    public void setActivityStarterImpl(ActivityStarter starter) {
        this.mActualStarter = starter;
    }
}
