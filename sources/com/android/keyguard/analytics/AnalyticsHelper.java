package com.android.keyguard.analytics;

import android.util.Slog;
import com.android.systemui.miui.analytics.AnalyticsWrapper;
import java.util.HashMap;
import java.util.Map;
import miui.os.Build;

public class AnalyticsHelper {
    private static final boolean IS_INTERNATIONAL_BUILD = Build.IS_INTERNATIONAL_BUILD;

    public static void recordDownloadLockScreenMagazine(String source) {
        Map<String, String> params = new HashMap<>();
        params.put("source", source);
        track("systemui_keyguard", "keyguard_download_lockscreen_magazine", params);
    }

    public static void recordUnlockWay(String way) {
        Slog.w("miui_keyguard", "unlock keyguard by " + way);
        Map<String, String> params = new HashMap<>();
        params.put("way", way);
        track("systemui_keyguard", "keyguard_unlock_way", params);
    }

    public static void recordLeftViewItem(String item, String action) {
        Map<String, String> params = new HashMap<>();
        params.put("action", action);
        track("systemui_keyguard", item, params);
    }

    public static void recordEnterLeftview(boolean isLockScreenWallpaperOpen) {
        Map<String, String> params = new HashMap<>();
        params.put("is_lockscreen_wallpaper_open", String.valueOf(isLockScreenWallpaperOpen));
        track("systemui_keyguard", "keyguard_enter_left_view", params);
    }

    public static void recordLeftLockscreenMagazineButton(boolean isLockScreenWallpaperOpen) {
        Map<String, String> params = new HashMap<>();
        params.put("is_lockscreen_wallpaper_open", String.valueOf(isLockScreenWallpaperOpen));
        track("systemui_keyguard", "keyguard_left_view_lockscreen_magazine_button", params);
    }

    public static void recordScreenOn(boolean isLockScreenWallpaperOpen) {
        Map<String, String> params = new HashMap<>();
        params.put("is_lockscreen_wallpaper_open", String.valueOf(isLockScreenWallpaperOpen));
        track("systemui_keyguard", "keyguard_screenon", params);
    }

    public static void trackFaceUnlockFailCount(boolean hasFace) {
        Map<String, String> params = new HashMap<>();
        params.put("hasface", String.valueOf(hasFace));
        track("systemui_keyguard", "face_unlock_state_fail_count", params);
    }

    public static void trackFaceUnlockLocked() {
        record("systemui_keyguard", "face_unlock_locked");
    }

    public static void trackCheckPasswordFailedException(String exception) {
        Map<String, String> params = new HashMap<>();
        params.put("exception", exception);
        track("systemui_keyguard", "keyguard_check_password_failed_exception", params);
    }

    public static void recordWrongSunChangePoint(String detail) {
        Map<String, String> params = new HashMap<>();
        params.put("aod_wrong_detail", detail);
        track("aod_wrong_sun_change_point", params);
    }

    public static void track(String key, Map<String, String> parameters) {
        track("systemui_keyguard", key, parameters);
    }

    public static void track(String category, String key, Map<String, String> parameters) {
        if (IS_INTERNATIONAL_BUILD) {
            AnalyticsWrapper.recordCountEventAnonymous(category, key, parameters);
        } else {
            AnalyticsWrapper.recordCountEvent(category, key, parameters);
        }
    }

    public static void record(String key) {
        record("systemui_keyguard", key);
    }

    public static void record(String category, String key) {
        if (IS_INTERNATIONAL_BUILD) {
            AnalyticsWrapper.recordCountEventAnonymous(category, key);
        } else {
            AnalyticsWrapper.recordCountEvent(category, key);
        }
    }

    public static void recordEnum(String key, String value) {
        recordEnum("systemui_keyguard", key, value);
    }

    public static void recordEnum(String category, String key, String value) {
        if (IS_INTERNATIONAL_BUILD) {
            AnalyticsWrapper.recordStringPropertyEventAnonymous(category, key, value);
        } else {
            AnalyticsWrapper.recordStringPropertyEvent(category, key, value);
        }
    }

    public static void recordCalculateEvent(String key, long value) {
        if (IS_INTERNATIONAL_BUILD) {
            AnalyticsWrapper.recordCalculateEventAnonymous("systemui_keyguard", key, value);
        } else {
            AnalyticsWrapper.recordCalculateEvent("systemui_keyguard", key, value);
        }
    }
}
