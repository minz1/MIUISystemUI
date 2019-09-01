package android.service.notification;

import android.app.Notification;
import android.service.notification.NotificationListenerService;

public class NotificationListenerServiceCompat {
    public static boolean shouldSuppressScreenOff(NotificationListenerService.Ranking ranking) {
        return (ranking.getSuppressedVisualEffects() & 1) != 0;
    }

    public static boolean shouldSuppressScreenOn(NotificationListenerService.Ranking ranking) {
        return (ranking.getSuppressedVisualEffects() & 2) != 0;
    }

    public static int getImportance(NotificationListenerService.Ranking ranking) {
        return ranking.getImportance();
    }

    public static int getImportance(Notification notification) {
        return -1000;
    }

    public static String getOverrideGroupKey(NotificationListenerService.Ranking ranking) {
        return ranking.getOverrideGroupKey();
    }
}
