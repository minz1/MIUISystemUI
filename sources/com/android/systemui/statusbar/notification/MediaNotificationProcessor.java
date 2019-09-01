package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.app.NotificationCompat;

public class MediaNotificationProcessor {
    public static void processNotification(Notification notification, Notification.Builder builder) {
        if (notification.getLargeIcon() != null) {
            NotificationCompat.setRebuildStyledRemoteViews(builder, true);
        }
    }
}
