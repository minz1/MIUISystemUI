package com.android.systemui.util;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Pair;
import com.android.systemui.R;
import java.util.List;

public final class ApplicationInfoHelper {
    public static void postEphemeralNotificationIfNeeded(Context context, String pkg, int userId, ApplicationInfo appInfo, NotificationManager noMan, int taskId, ArraySet<Pair<String, Integer>> outSet) {
        if (appInfo.isInstantApp()) {
            postEphemeralNotif(context, pkg, userId, appInfo, noMan, taskId, outSet);
        }
    }

    private static void postEphemeralNotif(Context context, String pkg, int userId, ApplicationInfo appInfo, NotificationManager noMan, int taskId, ArraySet<Pair<String, Integer>> outSet) {
        ComponentName aiaComponent;
        Context context2 = context;
        String str = pkg;
        int i = userId;
        ApplicationInfo applicationInfo = appInfo;
        Bundle extras = new Bundle();
        extras.putString("android.substName", context2.getString(R.string.instant_apps));
        outSet.add(new Pair(str, Integer.valueOf(userId)));
        String message = context2.getString(R.string.instant_apps_message);
        PendingIntent appInfoAction = PendingIntent.getActivity(context2, 0, new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", str, null)), 0);
        Notification.Action action = new Notification.Action.Builder(null, context2.getString(R.string.app_info), appInfoAction).build();
        Intent browserIntent = getTaskIntent(context2, taskId, i);
        Notification.Builder builder = NotificationCompat.newBuilder(context2, NotificationChannels.GENERAL);
        if (browserIntent != null) {
            browserIntent.setComponent(null).setPackage(null).addFlags(512).addFlags(268435456);
            PendingIntent pendingIntent = PendingIntent.getActivity(context2, 0, browserIntent, 0);
            try {
                aiaComponent = AppGlobals.getPackageManager().getInstantAppInstallerComponent();
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
                aiaComponent = null;
            }
            Intent addCategory = new Intent().setComponent(aiaComponent).setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE");
            StringBuilder sb = new StringBuilder();
            ComponentName componentName = aiaComponent;
            sb.append("unique:");
            Intent intent = browserIntent;
            sb.append(System.currentTimeMillis());
            builder.addAction(new Notification.Action.Builder(null, context2.getString(R.string.go_to_web), PendingIntent.getActivity(context2, 0, addCategory.addCategory(sb.toString()).putExtra("android.intent.extra.PACKAGE_NAME", applicationInfo.packageName).putExtra("android.intent.extra.VERSION_CODE", applicationInfo.versionCode).putExtra("android.intent.extra.EPHEMERAL_FAILURE", pendingIntent), 0)).build());
        }
        noMan.notifyAsUser(str, 7, builder.addExtras(extras).addAction(action).setContentIntent(appInfoAction).setColor(context2.getColor(R.color.instant_apps_color)).setContentTitle(applicationInfo.loadLabel(context.getPackageManager())).setLargeIcon(Icon.createWithResource(str, applicationInfo.icon)).setSmallIcon(Icon.createWithResource(context.getPackageName(), R.drawable.instant_icon)).setContentText(message).setOngoing(true).build(), new UserHandle(i));
    }

    private static Intent getTaskIntent(Context context, int taskId, int userId) {
        try {
            int i = 0;
            List<ActivityManager.RecentTaskInfo> tasks = ActivityManagerCompat.getRecentTasksForUser((ActivityManager) context.getSystemService(ActivityManager.class), 5, 0, userId);
            while (true) {
                int i2 = i;
                if (i2 >= tasks.size()) {
                    break;
                } else if (tasks.get(i2).id == taskId) {
                    return tasks.get(i2).baseIntent;
                } else {
                    i = i2 + 1;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}
