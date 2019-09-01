package com.android.internal.widget;

import android.view.View;

public class NotificationActionCompat {
    public static void setEmphasizedNotificationButtonBgIfNeed(View view) {
        if (view instanceof EmphasizedNotificationButton) {
            view.setBackgroundColor(0);
        }
    }

    public static void removeCompoundDrawableIfNeed(View view) {
    }
}
