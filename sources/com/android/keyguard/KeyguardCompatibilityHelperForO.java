package com.android.keyguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.view.WindowManager;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.systemui.Dependency;
import com.android.systemui.miui.AppIconsManager;
import java.util.ArrayList;
import miui.hardware.display.DisplayFeatureManager;

public class KeyguardCompatibilityHelperForO {
    private static ArrayList<IKeyguardDismissCallback> sDismissCallbacks = new ArrayList<>();

    public static void sendNotification(Context context, String title, String content, int icon, Intent intent, boolean autoCancel, String targetPkg) {
        NotificationManager manager = (NotificationManager) context.getSystemService("notification");
        manager.createNotificationChannel(new NotificationChannel("MiuiKeyguard", title, 4));
        Notification notification = new Notification.Builder(context).setContentTitle(title).setContentText(content).setSmallIcon(icon).setLargeIcon(((AppIconsManager) Dependency.get(AppIconsManager.class)).getAppIconBitmap(context, targetPkg)).setChannelId("MiuiKeyguard").setContentIntent(PendingIntent.getActivityAsUser(context, 0, intent, 0, null, UserHandle.CURRENT)).setAutoCancel(autoCancel).build();
        notification.extraNotification.setTargetPkg("android");
        notification.flags = 16;
        manager.notifyAsUser(null, icon, notification, UserHandle.CURRENT);
    }

    public static void setScreenEffect(int mode, int value) {
        DisplayFeatureManager.getInstance().setScreenEffect(mode, value);
    }

    public static void setFlag(WindowManager.LayoutParams lp) {
        lp.privateFlags |= 16;
    }

    public static void setRoundedCornersOverlayFlag(WindowManager.LayoutParams lp) {
        lp.privateFlags |= 1048576;
    }
}
