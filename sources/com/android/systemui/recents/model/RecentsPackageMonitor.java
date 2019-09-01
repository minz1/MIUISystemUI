package com.android.systemui.recents.model;

import android.content.Context;
import android.os.UserHandle;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;

public class RecentsPackageMonitor extends PackageMonitor {
    public void register(Context context) {
        try {
            register(context, BackgroundThread.get().getLooper(), UserHandle.ALL, true);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        try {
            RecentsPackageMonitor.super.unregister();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void onPackageRemoved(String packageName, int uid) {
        RecentsEventBus.getDefault().post(new PackagesChangedEvent(this, packageName, getChangingUserId()));
    }

    public boolean onPackageChanged(String packageName, int uid, String[] components) {
        onPackageModified(packageName);
        return true;
    }

    public void onPackageModified(String packageName) {
        RecentsEventBus.getDefault().post(new PackagesChangedEvent(this, packageName, getChangingUserId()));
    }
}
