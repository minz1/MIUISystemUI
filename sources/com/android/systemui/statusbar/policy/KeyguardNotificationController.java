package com.android.systemui.statusbar.policy;

public interface KeyguardNotificationController {
    void add(String str);

    void clearAll();

    void delete(String str);

    void setListener(NotificationChangeListener notificationChangeListener);

    void update(String str);
}
