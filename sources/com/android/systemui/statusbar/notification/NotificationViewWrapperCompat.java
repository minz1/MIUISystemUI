package com.android.systemui.statusbar.notification;

import android.view.NotificationHeaderView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.ImageFloatingTextView;

public class NotificationViewWrapperCompat {
    public static TextView findAppNameTextView(View view) {
        return (TextView) view.findViewById(16908721);
    }

    public static TextView findHeaderTextView(View view) {
        return (TextView) view.findViewById(16908947);
    }

    public static TextView findHeaderTextDividerView(View view) {
        return (TextView) view.findViewById(16908948);
    }

    public static ImageView findExpandButtonView(View view) {
        return (ImageView) view.findViewById(16908871);
    }

    public static NotificationHeaderView findNotificationHeaderView(View view) {
        return view.findViewById(16909137);
    }

    public static boolean isNotificationHeader(View view) {
        return view.getId() == 16909137;
    }

    public static View findNotificationMessagingView(View view) {
        return view.findViewById(16909145);
    }

    public static TextView findInboxText0View(View view) {
        return (TextView) view.findViewById(16908980);
    }

    public static Object getRemoteInputTag(View view) {
        return view.getTag(16909257);
    }

    public static void setHasImage(TextView bigText, boolean hasImage) {
        if (bigText instanceof ImageFloatingTextView) {
            ((ImageFloatingTextView) bigText).setHasImage(hasImage);
        }
    }
}
