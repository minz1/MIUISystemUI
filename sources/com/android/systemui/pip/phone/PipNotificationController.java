package com.android.systemui.pip.phone;

import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.util.NotificationChannels;

public class PipNotificationController {
    private static final String NOTIFICATION_TAG = PipNotificationController.class.getName();
    private static final String TAG = PipNotificationController.class.getSimpleName();
    private IActivityManager mActivityManager;
    private AppOpsManager.OnOpChangedListener mAppOpsChangedListener = new AppOpsManager.OnOpChangedListener() {
        public void onOpChanged(String op, String packageName) {
            try {
                if (PipNotificationController.this.mAppOpsManager.checkOpNoThrow(67, PipNotificationController.this.mContext.getPackageManager().getApplicationInfo(packageName, 0).uid, packageName) != 0) {
                    PipNotificationController.this.mMotionHelper.dismissPip();
                }
            } catch (PackageManager.NameNotFoundException e) {
                PipNotificationController.this.unregisterAppOpsListener();
            }
        }
    };
    /* access modifiers changed from: private */
    public AppOpsManager mAppOpsManager;
    /* access modifiers changed from: private */
    public Context mContext;
    private String mDeferredNotificationPackageName;
    /* access modifiers changed from: private */
    public PipMotionHelper mMotionHelper;
    private NotificationManager mNotificationManager;

    public PipNotificationController(Context context, IActivityManager activityManager, PipMotionHelper motionHelper) {
        this.mContext = context;
        this.mActivityManager = activityManager;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mNotificationManager = NotificationManager.from(context);
        this.mMotionHelper = motionHelper;
    }

    public void onActivityPinned(String packageName, boolean deferUntilAnimationEnds) {
        this.mNotificationManager.cancel(NOTIFICATION_TAG, 0);
        if (deferUntilAnimationEnds) {
            this.mDeferredNotificationPackageName = packageName;
        } else {
            showNotificationForApp(this.mDeferredNotificationPackageName);
        }
        registerAppOpsListener(packageName);
    }

    public void onPinnedStackAnimationEnded() {
        if (this.mDeferredNotificationPackageName != null) {
            showNotificationForApp(this.mDeferredNotificationPackageName);
            this.mDeferredNotificationPackageName = null;
        }
    }

    public void onActivityUnpinned(ComponentName topPipActivity) {
        unregisterAppOpsListener();
        this.mDeferredNotificationPackageName = null;
        if (topPipActivity != null) {
            onActivityPinned(topPipActivity.getPackageName(), false);
        } else {
            this.mNotificationManager.cancel(NOTIFICATION_TAG, 0);
        }
    }

    private void showNotificationForApp(String packageName) {
        Notification.Builder builder = new Notification.Builder(this.mContext, NotificationChannels.GENERAL).setLocalOnly(true).setOngoing(true).setSmallIcon(R.drawable.pip_notification_icon).setColor(this.mContext.getColor(17170799));
        if (updateNotificationForApp(builder, packageName)) {
            SystemUI.overrideNotificationAppName(this.mContext, builder);
            this.mNotificationManager.notify(NOTIFICATION_TAG, 0, builder.build());
        }
    }

    private boolean updateNotificationForApp(Notification.Builder builder, String packageName) {
        Icon appIcon;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (appInfo == null) {
                return false;
            }
            String appName = pm.getApplicationLabel(appInfo).toString();
            String message = this.mContext.getString(R.string.pip_notification_message, new Object[]{appName});
            Intent settingsIntent = new Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.fromParts("package", packageName, null));
            settingsIntent.setFlags(268468224);
            if (appInfo.icon != 0) {
                appIcon = Icon.createWithResource(packageName, appInfo.icon);
            } else {
                appIcon = Icon.createWithResource(Resources.getSystem(), 17301651);
            }
            builder.setContentTitle(this.mContext.getString(R.string.pip_notification_title, new Object[]{appName})).setContentText(message).setContentIntent(PendingIntent.getActivity(this.mContext, packageName.hashCode(), settingsIntent, 268435456)).setStyle(new Notification.BigTextStyle().bigText(message)).setLargeIcon(appIcon);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not update notification for application", e);
            return false;
        }
    }

    private void registerAppOpsListener(String packageName) {
        this.mAppOpsManager.startWatchingMode(67, packageName, this.mAppOpsChangedListener);
    }

    /* access modifiers changed from: private */
    public void unregisterAppOpsListener() {
        this.mAppOpsManager.stopWatchingMode(this.mAppOpsChangedListener);
    }
}
