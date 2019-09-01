package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.miui.statusbar.notification.FoldBucketHelper;
import com.android.systemui.miui.statusbar.phone.rank.PackageScoreCache;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import miui.os.Build;

public class AnalyticsHelper {
    private static BroadcastReceiver mAnalyticsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("AnalyticsHelper", "mAnalyticsReceiver onReceive(): " + action);
            if ("com.android.systemui.action_track_settings_app_notifications".equals(action)) {
                AnalyticsHelper.trackSettingsAppNotifications();
            } else if ("com.android.systemui.action_track_settings_toggle_positions".equals(action)) {
                AnalyticsHelper.trackSettingsTogglePositions(intent.getStringExtra("from"));
            } else if ("com.android.systemui.action_track_settings_count_event".equals(action)) {
                String type = intent.getStringExtra("settings_guide");
                Map<String, String> map = new HashMap<>();
                map.put("type", type);
                AnalyticsHelper.trackSettingsCountEvent("systemui_fullscreen_guide_click", map);
            }
        }
    };
    private static Calendar sCalendar = Calendar.getInstance();
    private static long sExpandStartTime = 0;
    private static Map<String, String> sStatusBarExpandParam = new HashMap();

    public static void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.systemui.action_track_settings_app_notifications");
        filter.addAction("com.android.systemui.action_track_settings_toggle_positions");
        filter.addAction("com.android.systemui.action_track_settings_count_event");
        context.registerReceiverAsUser(mAnalyticsReceiver, UserHandle.ALL, filter, null, null);
    }

    public static void track(String category, String key, Map<String, String> parameters) {
        parameters.put("page", NotificationPanelView.sQsExpanded ? "dual" : "single");
        parameters.put("alogrithm", String.valueOf(FoldBucketHelper.getFoldBucket()));
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).recordCountEventAnonymous(category, key, parameters);
    }

    public static void trackProperty(String category, String key, String value) {
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).recordStringPropertyEventAnonymous(category, key, value);
    }

    public static void trackQSTilesClick(String tileSpec, boolean inCustomizer) {
        if (!filterCustomTile(tileSpec)) {
            Map<String, String> param = new HashMap<>();
            param.put("tileSpec", tileSpec);
            param.put("inCustomizer", String.valueOf(inCustomizer));
            track("systemui_qs_tiles", "systemui_qs_tiles_click", param);
        }
    }

    public static void trackQSTilesCount(int count) {
        trackProperty("systemui_qs_tiles", "systemui_qs_tiles_count", String.valueOf(count));
    }

    public static void trackQSTilesOrderChange(int count) {
        Map<String, String> param = new HashMap<>();
        param.put("count", String.valueOf(count));
        track("systemui_qs_tiles", "systemui_qs_tiles_order_change", param);
    }

    public static void trackQSTilesLongClick(String tileSpec) {
        if (!filterCustomTile(tileSpec)) {
            Map<String, String> param = new HashMap<>();
            param.put("tileSpec", tileSpec);
            track("systemui_qs_tiles", "systemui_qs_tiles_long_click", param);
        }
    }

    public static void trackQSTilesSecondaryClick(String tileSpec) {
        if (!filterCustomTile(tileSpec)) {
            Map<String, String> param = new HashMap<>();
            param.put("tileSpec", tileSpec);
            track("systemui_toggles", "systemui_qs_tiles_secondary_click", param);
        }
    }

    private static boolean filterCustomTile(String tileSpec) {
        return tileSpec.startsWith("custom(");
    }

    public static void trackNotificationBlock(String pkg) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("pkg", pkg);
        parameters.put("block_time", getHour());
        parameters.put("block_from", "settings");
        track("systemui_notifications", "systemui_notification_block", parameters);
    }

    public static void trackNotificationBlock(String pkg, ExpandedNotification notification) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("pkg", pkg);
        parameters.put("base_pkg", notification.getBasePkg());
        parameters.put("block_time", getHour());
        parameters.put("when", "" + notification.getNotification().when);
        parameters.put("post_time", "" + notification.getPostTime());
        parameters.put("id", "" + notification.getId());
        parameters.put("tag", notification.getTag());
        parameters.put("block_from", "systemui");
        track("systemui_notifications", "systemui_notification_block", parameters);
    }

    public static void trackNotificationClick(String pkg, int index) {
        Map<String, String> param = new HashMap<>();
        param.put("pkg", pkg);
        param.put("time", getHour());
        param.put("index", index + "");
        track("systemui_notifications", "systemui_notification_click_google", param);
        ((PackageScoreCache) Dependency.get(PackageScoreCache.class)).addClick(pkg);
    }

    public static void trackActionClick(String pkg, int actionIndex) {
        Map<String, String> param = new HashMap<>();
        param.put("pkg", pkg);
        param.put("time", getHour());
        param.put("actionIndex", String.valueOf(actionIndex));
        track("systemui_notifications", "systemui_notification_action_click", param);
    }

    public static void trackSetImportance(String pkg, int newValue) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("pkg", pkg);
        parameters.put("newValue", newValue + "");
        track("systemui_notifications", "systemui_notification_set_importance", parameters);
    }

    public static void trackNotificationRemove(String pkg, String removeWay) {
        Map<String, String> param = new HashMap<>();
        param.put("pkg", pkg);
        param.put("removeWay", removeWay);
        track("systemui_notifications", "systemui_notification_remove", param);
    }

    public static void trackNotificationClearAll(int clearableCount) {
        Map<String, String> param = new HashMap<>();
        param.put("actualClearCount", clearableCount + "");
        track("systemui_notifications", "systemui_notification_clear_all", param);
    }

    public static void trackFoldClick() {
        Map<String, String> param = new HashMap<>();
        param.put("time", getHour());
        track("systemui_notifications", "systemui_fold_click", param);
    }

    private static String getHour() {
        sCalendar.setTimeInMillis(System.currentTimeMillis());
        return String.valueOf(sCalendar.get(10));
    }

    public static void trackStatusBarCollapse(String collapseWay) {
        if (Build.IS_DEVELOPMENT_VERSION) {
            Map<String, String> param = new HashMap<>();
            param.put("collapseWay", collapseWay);
            track("systemui_statusbar", "systemui_statusbar_collapse", param);
        }
    }

    public static void trackSettingsAppNotifications() {
        track("systemui_settings", "systemui_settings_app_notifications", new HashMap());
    }

    public static void trackSettingsTogglePositions(String from) {
        Map<String, String> param = new HashMap<>();
        param.put("from", from);
        track("systemui_settings", "systemui_settings_toggle_positions", param);
    }

    public static void trackSettingsNotificationStyle(int style) {
        Map<String, String> param = new HashMap<>();
        param.put("notification_style", String.valueOf(style));
        track("systemui_settings", "systemui_settings_notification_style", param);
    }

    public static void trackMaxAspectChangedEvent(String type, String pkg) {
        Map<String, String> map = new HashMap<>();
        map.put(type, pkg);
        trackSettingsCountEvent("systemui_settings_status", map);
    }

    public static void trackSettingsCountEvent(String key, Map<String, String> map) {
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).recordCountEventAnonymous("systemui_settings", key, map);
    }

    public static void trackSettings(String key, Map<String, String> map) {
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).recordCountEventAnonymous("systemui_settings", key, map);
    }
}
