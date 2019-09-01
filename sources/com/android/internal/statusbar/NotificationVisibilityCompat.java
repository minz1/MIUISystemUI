package com.android.internal.statusbar;

public class NotificationVisibilityCompat {
    public static NotificationVisibility obtain(String key, int rank, int count, boolean visible) {
        return NotificationVisibility.obtain(key, rank, count, visible);
    }
}
