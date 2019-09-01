package com.android.keyguard.analytics;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONObject;

public class LockScreenMagazineAnalytics {
    private static void record(String key, Map<String, String> parameters) {
        Log.d("LockScreenMagazineAnalytics", "record key = " + key + " parameters = " + parameters);
        AnalyticsHelper.track(key, parameters);
    }

    private static HashMap getBaseParams() {
        HashMap<String, String> params = new HashMap<>();
        params.put("lockScreenMagazineStatus", WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() ? "open" : "close");
        params.put("currentLanguage", Locale.getDefault().getLanguage());
        return params;
    }

    public static void recordLockScreenWallperProviderChanged(Context context, String provider) {
        HashMap<String, String> params = getBaseParams();
        params.put("provider", provider);
        record("lock_screen_wallpaper_provider_changed", params);
    }

    public static void recordLockScreenWallperProviderStatus() {
        HashMap<String, String> params = getBaseParams();
        params.put("status", WallpaperAuthorityUtils.getWallpaperAuthority());
        record("lock_screen_magazine_open_status", params);
    }

    public static void recordThemeType(Context context, boolean isDefault) {
        HashMap<String, String> params = getBaseParams();
        params.put("isDefault", isDefault ? "default" : "tripartite");
        record("lock_screen_theme_type", params);
    }

    public static void recordLockScreenMagazinePreviewAction(Context context, String action) {
        Map<String, String> params = getBaseParams();
        params.put("action", action);
        record("keyguard_preview_button", params);
    }

    public static void recordNegativeStatus(Context context) {
        Map<String, String> params = getBaseParams();
        params.put("status", KeyguardUpdateMonitor.getInstance(context).isSupportLockScreenMagazineLeft() ? "lockScreenMagazine" : "controlCenter");
        record("lock_screen_negative_status", params);
    }

    public static void recordLockScreenMagazineAd(Context context) {
        LockScreenMagazineWallpaperInfo lockScreenMagazineWallpaperInfo = KeyguardUpdateMonitor.getInstance(context).getLockScreenMagazineWallpaperInfo();
        if (lockScreenMagazineWallpaperInfo != null) {
            String authority = lockScreenMagazineWallpaperInfo.authority;
            if (WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() && "com.xiaomi.ad.LockScreenAdProvider".equals(authority)) {
                ContentResolver contentResolver = context.getContentResolver();
                IContentProvider provider = contentResolver.acquireProvider(Uri.parse("content://" + authority));
                if (provider != null) {
                    try {
                        JSONObject jo = new JSONObject();
                        jo.put("key", lockScreenMagazineWallpaperInfo.key);
                        jo.put("event", 1);
                        Bundle extras = new Bundle();
                        extras.putString("request_json", jo.toString());
                        provider.call(context.getPackageName(), "recordEvent", null, extras);
                    } catch (Exception e) {
                        Log.e("miui_keyguard", e.toString());
                    } catch (Throwable th) {
                        context.getContentResolver().releaseProvider(provider);
                        throw th;
                    }
                    context.getContentResolver().releaseProvider(provider);
                }
            }
        }
    }
}
