package android.app;

import android.app.Notification;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.widget.RemoteViews;

public class NotificationCompat {
    public static boolean isMediaNotification(Notification notification) {
        return notification.isMediaNotification();
    }

    public static boolean suppressAlertingDueToGrouping(Notification notification) {
        return notification.suppressAlertingDueToGrouping();
    }

    public static boolean isColorized(Notification notification) {
        return notification.isColorized();
    }

    public static Notification.Builder newBuilder(Context context, String channelId) {
        return new Notification.Builder(context, channelId);
    }

    public static String getChannelId(Notification notification) {
        return notification.getChannelId();
    }

    public static RemoteViews makeAmbientNotification(Notification.Builder builder, boolean redactAmbient) {
        if (redactAmbient) {
            return builder.makePublicAmbientNotification();
        }
        return builder.makeAmbientNotification();
    }

    public static RemoteViews createHeadsUpContentView(Notification.Builder builder, boolean usesIncreasedHeadsUpHeight) {
        return builder.createHeadsUpContentView(usesIncreasedHeadsUpHeight);
    }

    public static RemoteViews createContentView(Notification.Builder builder, boolean isLowPriority, boolean useLarge) {
        if (isLowPriority) {
            return builder.makeLowPriorityContentView(false);
        }
        return builder.createContentView(useLarge);
    }

    public static RemoteViews makeNotificationHeader(Notification.Builder builder, boolean ambient) {
        return builder.makeNotificationHeader(ambient);
    }

    public static RemoteViews makeLowPriorityContentView(Notification.Builder builder, boolean useRegularSubtext) {
        return builder.makeLowPriorityContentView(useRegularSubtext);
    }

    public static void setRebuildStyledRemoteViews(Notification.Builder builder, boolean rebuild) {
        builder.setRebuildStyledRemoteViews(rebuild);
    }

    public static void setBackgroundColorHint(Notification.Builder builder, int backgroundColor) {
    }

    public static void makeHeaderExpanded(RemoteViews result) {
        Notification.Builder.makeHeaderExpanded(result);
    }

    public static void snoozeNotification(NotificationListenerService service, String key, String id) {
        service.snoozeNotification(key, id);
    }

    public static void snoozeNotification(NotificationListenerService service, String key, long duration) {
        service.snoozeNotification(key, duration);
    }

    public static boolean showsTime(Notification notification) {
        return notification.when != 0 && notification.extras.getBoolean("android.showWhen");
    }

    public static boolean showsChronometer(Notification notification) {
        return notification.when != 0 && notification.extras.getBoolean("android.showChronometer");
    }

    public static Notification.Builder recoverBuilder(Context context, Notification n) {
        return Notification.Builder.recoverBuilder(context, n);
    }

    public static Notification.Builder setRemoteInputHistory(Notification.Builder b, CharSequence[] text) {
        return b.setRemoteInputHistory(text);
    }

    public static String loadHeaderAppName(Notification.Builder b) {
        return b.loadHeaderAppName();
    }

    public static void setChannelId(Notification.Builder builder, String channelId) {
        builder.setChannelId(channelId);
    }
}
