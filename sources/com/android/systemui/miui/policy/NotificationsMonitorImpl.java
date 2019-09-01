package com.android.systemui.miui.policy;

import android.service.notification.StatusBarNotification;
import com.android.systemui.miui.policy.NotificationsMonitor;
import java.util.ArrayList;
import java.util.List;

public class NotificationsMonitorImpl implements NotificationsMonitor {
    private final List<NotificationsMonitor.Callback> mCallbacks = new ArrayList();

    public void notifyNotificationAdded(StatusBarNotification entry) {
        for (NotificationsMonitor.Callback callback : new ArrayList<>(this.mCallbacks)) {
            callback.onNotificationAdded(entry);
        }
    }

    public void notifyNotificationArrived(StatusBarNotification entry) {
        for (NotificationsMonitor.Callback callback : new ArrayList<>(this.mCallbacks)) {
            callback.onNotificationArrived(entry);
        }
    }

    public void notifyNotificationUpdated(StatusBarNotification entry) {
        for (NotificationsMonitor.Callback callback : new ArrayList<>(this.mCallbacks)) {
            callback.onNotificationUpdated(entry);
        }
    }

    public void addCallback(NotificationsMonitor.Callback listener) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(listener);
        }
    }

    public void removeCallback(NotificationsMonitor.Callback listener) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(listener);
        }
    }
}
