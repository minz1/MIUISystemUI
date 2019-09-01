package com.android.systemui.util;

import android.app.NotificationChannelCompat;
import android.app.NotificationManager;
import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import java.util.Arrays;

public class NotificationChannels extends SystemUI {
    public static String ALERTS = "ALRT";
    public static String BATTERY = "BAT";
    public static String GENERAL = "GEN";
    public static String LOCATION = "LOC";
    public static String SCREENBUTTON = "SCB";
    public static String SCREENSHOTS = "SCN";
    public static String STORAGE = "DSK";
    public static String TVPIP = "TPP";
    public static String USB = "USB";

    @VisibleForTesting
    public static void createAll(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(NotificationManager.class);
        NotificationChannelCompat[] notificationChannelCompatArr = new NotificationChannelCompat[6];
        notificationChannelCompatArr[0] = new NotificationChannelCompat(SCREENSHOTS, context.getString(R.string.notification_channel_screenshot), 2);
        notificationChannelCompatArr[1] = new NotificationChannelCompat(GENERAL, context.getString(R.string.notification_channel_general), 1);
        notificationChannelCompatArr[2] = new NotificationChannelCompat(STORAGE, context.getString(R.string.notification_channel_storage), isTv(context) ? 3 : 2);
        notificationChannelCompatArr[3] = new NotificationChannelCompat(SCREENBUTTON, context.getString(R.string.notification_channel_screenbutton), 2);
        notificationChannelCompatArr[4] = new NotificationChannelCompat(LOCATION, context.getString(R.string.notification_channel_location), 2);
        notificationChannelCompatArr[5] = new NotificationChannelCompat(USB, context.getString(R.string.notification_channel_usb), 3);
        NotificationChannelCompat.createNotificationChannels(nm, Arrays.asList(notificationChannelCompatArr));
        NotificationChannelCompat batteryChannel = new NotificationChannelCompat(BATTERY, context.getString(R.string.notification_channel_battery), 4);
        batteryChannel.enableLights(true);
        NotificationChannelCompat.createNotificationChannel(nm, batteryChannel);
        NotificationChannelCompat alertChannel = new NotificationChannelCompat(ALERTS, context.getString(R.string.notification_channel_alerts), 4);
        alertChannel.enableVibration(true);
        NotificationChannelCompat.createNotificationChannel(nm, alertChannel);
        if (isTv(context)) {
            NotificationChannelCompat.createNotificationChannel(nm, new NotificationChannelCompat(TVPIP, context.getString(R.string.notification_channel_tv_pip), 5));
        }
    }

    public void start() {
        createAll(this.mContext);
    }

    private static boolean isTv(Context context) {
        return context.getPackageManager().hasSystemFeature("android.software.leanback");
    }
}
