package android.view;

public class NotificationHeaderViewCompat {
    public static void setIconForceHidden(NotificationHeaderView headerView, boolean forceHidden) {
        if (headerView != null) {
            headerView.getIcon().setForceHidden(forceHidden);
        }
    }
}
