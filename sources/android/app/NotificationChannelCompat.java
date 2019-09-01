package android.app;

import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationChannelCompat {
    /* access modifiers changed from: private */
    public NotificationChannel mChannel;

    public NotificationChannelCompat(String id, CharSequence name, int importance) {
        this.mChannel = new NotificationChannel(id, name, importance);
    }

    private NotificationChannelCompat(NotificationChannel channel) {
        this.mChannel = channel;
    }

    public String getId() {
        return this.mChannel.getId();
    }

    public CharSequence getName() {
        return this.mChannel.getName();
    }

    public int getImportance() {
        return this.mChannel.getImportance();
    }

    public void enableVibration(boolean enable) {
        this.mChannel.enableVibration(enable);
    }

    public void enableLights(boolean enable) {
        this.mChannel.enableLights(enable);
    }

    public static void createNotificationChannel(NotificationManager nm, NotificationChannelCompat channel) {
        nm.createNotificationChannel(channel.mChannel);
    }

    public static void createNotificationChannels(NotificationManager nm, List<NotificationChannelCompat> channels) {
        nm.createNotificationChannels((List) channels.stream().map($$Lambda$NotificationChannelCompat$bbrrKGLqKYuZ8aturWndttVJIEA.INSTANCE).collect(Collectors.toList()));
    }

    public static NotificationChannelCompat getChannel(NotificationListenerService.Ranking ranking) {
        return new NotificationChannelCompat(ranking.getChannel());
    }

    public static int getNumNotificationChannelsForPackage(INotificationManager notificationManager, String pkg, int appUid, boolean includeDeleted) throws RemoteException {
        return notificationManager.getNumNotificationChannelsForPackage(pkg, appUid, includeDeleted);
    }

    public static CharSequence getGroupName(NotificationChannelCompat channel, INotificationManager notificationManager, String pkg, int appUid) throws RemoteException {
        if (channel.mChannel.getGroup() != null) {
            NotificationChannelGroup notificationChannelGroup = notificationManager.getNotificationChannelGroupForPackage(channel.mChannel.getGroup(), pkg, appUid);
            if (notificationChannelGroup != null) {
                return notificationChannelGroup.getName();
            }
        }
        return null;
    }

    public static void saveImportance(NotificationChannelCompat channel, int selectedImportance, INotificationManager notificationManager, String pkg, int appUid) {
        channel.mChannel.setImportance(selectedImportance);
        channel.mChannel.lockFields(4);
        try {
            notificationManager.updateNotificationChannelForPackage(pkg, appUid, channel.mChannel);
        } catch (Exception e) {
        }
    }

    public int hashCode() {
        return this.mChannel.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.mChannel.equals(((NotificationChannelCompat) obj).mChannel);
    }
}
