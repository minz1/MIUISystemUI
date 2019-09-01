package android.service.notification;

public class StatusBarNotificationCompat {
    public static boolean isAutoGroupSummary(StatusBarNotification sbn) {
        return sbn.getId() == Integer.MAX_VALUE && "ranker_group".equals(sbn.getTag()) && "ranker_group".equals(sbn.getNotification().getGroup());
    }

    public static void setOverrideGroupKey(StatusBarNotification sbn, String overrideGroupKey) {
        sbn.setOverrideGroupKey(overrideGroupKey);
    }

    public static String getOverrideGroupKey(StatusBarNotification sbn) {
        return sbn.getOverrideGroupKey();
    }

    public static boolean isGroup(StatusBarNotification sbn) {
        return sbn.isGroup();
    }

    public static boolean isAppGroup(StatusBarNotification sbn) {
        return sbn.isAppGroup();
    }
}
