package com.android.systemui.analytics;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.IWindowManager;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Constants;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import miui.os.MiuiInit;

public class SettingsJobSchedulerService extends JobService {
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mHasNavigationBar;

    public void onCreate() {
        super.onCreate();
        this.mHandlerThread = new HandlerThread("SettingsJobSchedulerService", 10);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        try {
            this.mHasNavigationBar = IWindowManager.Stub.asInterface(ServiceManager.getService("window")).hasNavigationBar();
        } catch (RemoteException e) {
        }
    }

    public void onDestroy() {
        super.onDestroy();
        this.mHandlerThread.quitSafely();
    }

    public boolean onStartJob(JobParameters jobParameters) {
        trackSettings();
        return false;
    }

    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void trackSettings() {
        this.mHandler.post(new Runnable() {
            public void run() {
                SettingsJobSchedulerService.this.settingsStatusMonitor();
            }
        });
    }

    /* access modifiers changed from: private */
    public void settingsStatusMonitor() {
        if (Constants.SUPPORT_AOD) {
            HashMap<String, String> map = new HashMap<>();
            map.put("aod_mode", "" + Settings.Secure.getInt(getContentResolver(), "aod_mode", -1));
            map.put("aod_mode_time", "" + Settings.Secure.getInt(getContentResolver(), "aod_mode_time", -1));
            AnalyticsHelper.trackSettings("aod", map);
        }
        if (Constants.IS_NOTCH && Build.VERSION.SDK_INT < 28) {
            HashMap<String, String> map2 = new HashMap<>();
            map2.put("force_black", "" + Settings.Global.getInt(getContentResolver(), "force_black", -1));
            AnalyticsHelper.trackSettings("notch", map2);
        }
        if (this.mHasNavigationBar) {
            HashMap<String, String> map3 = new HashMap<>();
            map3.put("fullScreen", String.valueOf(MiuiSettings.Global.getBoolean(getContentResolver(), "force_fsg_nav_bar")));
            boolean isAppSwitch = false;
            if (Settings.Global.getInt(getContentResolver(), "show_gesture_appswitch_feature", 0) != 0) {
                isAppSwitch = true;
            }
            map3.put("appswitch", String.valueOf(isAppSwitch));
            AnalyticsHelper.trackSettingsCountEvent("systemui_settings_status", map3);
            trackMaxAspectChangedApps();
        }
        HashMap<String, String> shortcut = new HashMap<>();
        String shortcutKey = Constants.IS_INTERNATIONAL ? "shortcut_international" : "shortcut";
        shortcut.put(shortcutKey, "" + Settings.Secure.getInt(getContentResolver(), "status_bar_notification_shade_shortcut", -1));
        AnalyticsHelper.trackSettings("notification_shade_shortcut", shortcut);
        AnalyticsHelper.trackSettingsNotificationStyle(NotificationUtil.getNotificationStyle());
    }

    private void trackMaxAspectChangedApps() {
        Intent mainIntent = new Intent("android.intent.action.MAIN");
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(mainIntent, 128);
        HashSet<String> pkgSet = new HashSet<>();
        for (ResolveInfo ri : resolveInfos) {
            String packageName = ri.activityInfo.packageName;
            String className = ri.activityInfo.name;
            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className) && !pkgSet.contains(packageName) && !getPackageName().equals(packageName)) {
                boolean restrict = MiuiInit.isRestrictAspect(packageName);
                int type = MiuiInit.getDefaultAspectType(packageName);
                pkgSet.add(packageName);
                if (type == 2 || type == 3 || type == 0) {
                    if (restrict) {
                        AnalyticsHelper.trackMaxAspectChangedEvent("maxaspect_off", packageName);
                    }
                } else if ((type == 5 || type == 4) && !restrict) {
                    AnalyticsHelper.trackMaxAspectChangedEvent("maxaspect_on", packageName);
                }
            }
        }
    }
}
