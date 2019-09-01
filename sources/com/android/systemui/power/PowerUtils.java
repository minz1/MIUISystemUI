package com.android.systemui.power;

import android.app.Notification;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.util.NotificationChannels;

public class PowerUtils {
    public static void enableSaveMode(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("POWER_SAVE_MODE_OPEN", true);
        bundle.putBoolean("LOW_BATTERY_DIALOG", true);
        try {
            context.getContentResolver().call(Uri.parse("content://com.miui.powercenter.powersaver"), "changePowerMode", null, bundle);
        } catch (IllegalArgumentException e) {
        }
    }

    public static void enableExtremeSaveMode(Context context, String source) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("EXTREME_POWER_SAVE_MODE_OPEN", true);
        bundle.putBoolean("IS_NOTIFY", true);
        bundle.putString("SOURCE", source);
        try {
            context.getContentResolver().call(Uri.parse("content://com.miui.powerkeeper.configure"), "changeExtremePowerMode", null, bundle);
        } catch (IllegalArgumentException e) {
            Log.e("PowerUtils", "enableExtremeSaveMode error", e);
        }
    }

    public static boolean isSaveModeEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), "POWER_SAVE_MODE_OPEN", 0, -2) != 0;
    }

    public static void trackLowBatteryDialog(Context context) {
        try {
            context.getContentResolver().call(Uri.parse("content://com.miui.powercenter.powersaver"), "showLowBatteryDialog", null, null);
        } catch (IllegalArgumentException e) {
        }
    }

    public static void showLowBatteryNotification(Context context, int batteryLevel) {
        int resId;
        Notification.Builder builder = new Notification.Builder(context);
        Intent powerCenterIntent = new Intent("miui.intent.action.POWER_MANAGER");
        if (batteryLevel <= 9) {
            resId = R.drawable.icon_9_percent;
        } else {
            resId = R.drawable.icon_19_percent;
        }
        int resId2 = resId;
        builder.setSmallIcon(R.drawable.powercenter_small_icon).setContentTitle(context.getString(R.string.notification_low_battery_title, new Object[]{Integer.valueOf(batteryLevel)})).setContentIntent(PendingIntent.getActivityAsUser(context, 0, powerCenterIntent, 0, null, UserHandle.CURRENT)).setAutoCancel(true).setShowWhen(true).setPriority(1);
        if (!Constants.IS_INTERNATIONAL) {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), resId2));
        }
        if (isExtremeSaveModeEnabable(context) || isSaveModeEnabled(context) || Constants.IS_TABLET) {
            builder.setContentText(context.getString(R.string.notification_low_battery_need_charge));
        } else {
            String actionText = context.getString(R.string.notification_low_battery_action_btn);
            Intent actionIntent = new Intent(context, PowerReceiver.class);
            actionIntent.setAction(PowerReceiver.ACTION_OPEN_SAVE_MODE);
            Notification.Action action = new Notification.Action(0, actionText, PendingIntent.getBroadcastAsUser(context, 0, actionIntent, 0, UserHandle.CURRENT));
            builder.addAction(action.icon, action.title, action.actionIntent);
            builder.setContentText(context.getString(R.string.notification_low_battery_open_save_mode));
            Bundle bundle = new Bundle();
            bundle.putBoolean("miui.showAction", !Constants.IS_INTERNATIONAL);
            builder.setExtras(bundle);
        }
        NotificationCompat.setChannelId(builder, NotificationChannels.BATTERY);
        Notification notification = builder.build();
        notification.flags |= 1;
        notification.defaults |= 4;
        notification.sound = null;
        notification.extraNotification.setCustomizedIcon(true);
        notification.extraNotification.setTargetPkg("android");
        ((NotificationManager) context.getSystemService("notification")).notifyAsUser(null, R.string.notification_low_battery_title, notification, UserHandle.CURRENT);
    }

    public static void hideLowBatteryNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancelAsUser(null, R.string.notification_low_battery_title, UserHandle.CURRENT);
    }

    public static boolean isExtremeSaveModeEnabable(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "EXTREME_POWER_MODE_ENABLE", 0) == 1;
    }
}
