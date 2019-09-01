package android.service.notification;

public abstract class AbstractStatusBarNotification extends StatusBarNotification {
    public AbstractStatusBarNotification(StatusBarNotification sbn) {
        super(sbn.getPackageName(), sbn.getPackageName(), sbn.getId(), sbn.getTag(), sbn.getUid(), sbn.getInitialPid(), sbn.getNotification(), sbn.getUser(), sbn.getOverrideGroupKey(), sbn.getPostTime());
    }

    public int getScore() {
        return getNotification().priority;
    }
}
