package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.AppIconsManager;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.util.NotificationChannels;
import com.android.systemui.util.Utils;
import com.android.systemui.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;

public class LocationControllerImpl extends BroadcastReceiver implements LocationController {
    private static final int[] mHighPowerRequestAppOpArray = {42};
    private AppOpsManager mAppOpsManager;
    /* access modifiers changed from: private */
    public boolean mAreActiveLocationRequests;
    private Context mContext;
    private final H mHandler = new H();
    private NotificationManager mNotificationManager;
    /* access modifiers changed from: private */
    public ArrayList<LocationController.LocationChangeCallback> mSettingsChangeCallbacks = new ArrayList<>();
    private StatusBarManager mStatusBarManager;

    private final class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    locationSettingsChanged();
                    return;
                case 2:
                    locationActiveChanged();
                    return;
                case 3:
                    locationStatusChanged((Intent) msg.obj);
                    return;
                default:
                    return;
            }
        }

        private void locationActiveChanged() {
            Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer<LocationController.LocationChangeCallback>() {
                public void accept(LocationController.LocationChangeCallback cb) {
                    cb.onLocationActiveChanged(LocationControllerImpl.this.mAreActiveLocationRequests);
                }
            });
        }

        private void locationSettingsChanged() {
            final boolean isEnabled = LocationControllerImpl.this.isLocationEnabled();
            Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer<LocationController.LocationChangeCallback>() {
                public void accept(LocationController.LocationChangeCallback cb) {
                    cb.onLocationSettingsChanged(isEnabled);
                }
            });
        }

        private void locationStatusChanged(Intent intent) {
            final Intent fintent = intent;
            Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer<LocationController.LocationChangeCallback>() {
                public void accept(LocationController.LocationChangeCallback cb) {
                    cb.onLocationStatusChanged(fintent);
                }
            });
        }
    }

    public LocationControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.GPS_ENABLED_CHANGE");
        filter.addAction("android.location.GPS_FIX_CHANGE");
        filter.addAction("android.location.HIGH_POWER_REQUEST_CHANGE");
        filter.addAction("android.location.MODE_CHANGED");
        context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, new Handler(bgLooper));
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mNotificationManager.cancelAsUser(null, 252119, UserHandle.CURRENT);
        updateActiveLocationRequests();
    }

    public void addCallback(LocationController.LocationChangeCallback cb) {
        this.mSettingsChangeCallbacks.add(cb);
        this.mHandler.sendEmptyMessage(1);
    }

    public void removeCallback(LocationController.LocationChangeCallback cb) {
        this.mSettingsChangeCallbacks.remove(cb);
    }

    public boolean isLocationEnabled() {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "location_mode", 0, ActivityManager.getCurrentUser()) != 0) {
            return true;
        }
        return false;
    }

    public boolean isLocationActive() {
        return this.mAreActiveLocationRequests;
    }

    /* access modifiers changed from: protected */
    public boolean areActiveHighPowerLocationRequests() {
        List<AppOpsManager.PackageOps> packages = this.mAppOpsManager.getPackagesForOps(mHighPowerRequestAppOpArray);
        if (packages != null) {
            int numPackages = packages.size();
            for (int packageInd = 0; packageInd < numPackages; packageInd++) {
                List<AppOpsManager.OpEntry> opEntries = packages.get(packageInd).getOps();
                if (opEntries != null) {
                    int numOps = opEntries.size();
                    for (int opInd = 0; opInd < numOps; opInd++) {
                        AppOpsManager.OpEntry opEntry = opEntries.get(opInd);
                        if (opEntry.getOp() == 42 && opEntry.isRunning()) {
                            return true;
                        }
                    }
                    continue;
                }
            }
        }
        return false;
    }

    private void updateActiveLocationRequests() {
        boolean hadActiveLocationRequests = this.mAreActiveLocationRequests;
        this.mAreActiveLocationRequests = areActiveHighPowerLocationRequests();
        if (this.mAreActiveLocationRequests != hadActiveLocationRequests) {
            this.mHandler.sendEmptyMessage(2);
        }
    }

    private void updateLocationStatus(Intent intent) {
        Message message = this.mHandler.obtainMessage();
        message.what = 3;
        message.obj = intent;
        this.mHandler.sendMessage(message);
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.location.HIGH_POWER_REQUEST_CHANGE".equals(action)) {
            updateActiveLocationRequests();
        } else if ("android.location.MODE_CHANGED".equals(action)) {
            this.mHandler.sendEmptyMessage(1);
        } else if ("android.location.GPS_ENABLED_CHANGE".equals(action) || "android.location.GPS_FIX_CHANGE".equals(action)) {
            updateLocationStatus(intent);
            updateGpsNotification(intent);
        }
    }

    private void updateGpsNotification(Intent intent) {
        int iconId;
        int textResId;
        boolean visible;
        Intent gpsIntent;
        Intent intent2 = intent;
        String action = intent.getAction();
        boolean enabled = intent2.getBooleanExtra("enabled", false);
        boolean onGoing = false;
        if (action.equals("android.location.GPS_FIX_CHANGE") && enabled) {
            iconId = 285344068;
            textResId = R.string.gps_notification_found_text;
            visible = true;
        } else if (!action.equals("android.location.GPS_ENABLED_CHANGE") || enabled) {
            iconId = Constants.SUPPORT_DUAL_GPS != 0 ? R.drawable.stat_sys_dual_gps_acquiring : R.drawable.stat_sys_gps_acquiring;
            textResId = R.string.gps_notification_searching_text;
            visible = true;
            onGoing = true;
        } else {
            visible = false;
            textResId = 0;
            iconId = 0;
        }
        int textResId2 = textResId;
        int iconId2 = iconId;
        boolean onGoing2 = onGoing;
        if (visible) {
            CharSequence text = null;
            String packageName = intent2.getStringExtra("android.intent.extra.PACKAGES");
            Icon icon = null;
            if (TextUtils.isEmpty(packageName)) {
                gpsIntent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
            } else {
                PackageManager pm = this.mContext.getPackageManager();
                try {
                    text = pm.getApplicationInfo(packageName, 0).loadLabel(pm);
                    Bitmap bitmap = ((AppIconsManager) Dependency.get(AppIconsManager.class)).getAppIconBitmap(this.mContext, packageName);
                    if (bitmap != null) {
                        icon = Icon.createWithBitmap(bitmap);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
                gpsIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", packageName, null));
            }
            gpsIntent.setFlags(268435456);
            Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.LOCATION).setContentTitle(this.mContext.getText(textResId2)).setContentText(text).setOngoing(onGoing2).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, gpsIntent, 0, null, UserHandle.CURRENT));
            if (icon == null) {
                builder.setSmallIcon(iconId2);
            } else {
                builder.setSmallIcon(icon);
            }
            Notification n = builder.build();
            if (!TextUtils.isEmpty(packageName)) {
                n.extraNotification.setTargetPkg(packageName);
                Intent intent3 = gpsIntent;
            } else {
                builder.setLargeIcon(BitmapFactory.decodeResource(this.mContext.getResources(), Constants.SUPPORT_DUAL_GPS ? R.drawable.notification_dual_gps : R.drawable.notification_gps));
                n.extraNotification.setCustomizedIcon(true);
            }
            n.extraNotification.setEnableKeyguard(false);
            n.extraNotification.setEnableFloat(false);
            n.tickerView = null;
            n.tickerText = null;
            String str = action;
            this.mNotificationManager.notifyAsUser(null, 252119, n, UserHandle.CURRENT);
            return;
        }
        this.mNotificationManager.cancelAsUser(null, 252119, UserHandle.CURRENT);
    }
}
