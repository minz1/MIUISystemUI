package com.android.systemui.statusbar.policy;

public class KeyguardNotificationControllerImpl implements KeyguardNotificationController {
    private NotificationChangeListener mNotificationListener;

    public void update(String pkg) {
        if (this.mNotificationListener != null) {
            this.mNotificationListener.onUpdate(pkg);
        }
    }

    public void clearAll() {
        if (this.mNotificationListener != null) {
            this.mNotificationListener.onClearAll();
        }
    }

    public void add(String pkg) {
        if (this.mNotificationListener != null) {
            this.mNotificationListener.onAdd(pkg);
        }
    }

    public void delete(String pkg) {
        if (this.mNotificationListener != null) {
            this.mNotificationListener.onDelete(pkg);
        }
    }

    public void setListener(NotificationChangeListener listener) {
        this.mNotificationListener = listener;
    }
}
