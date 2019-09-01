package com.android.keyguard.wallpaper;

import android.app.WallpaperInfo;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.WallpaperManagerCompat;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import miui.os.Build;

public class WallpaperAuthorityUtils {
    public static final String APPLY_MAGAZINE_DEFAULT_AUTHORITY = LockScreenMagazineUtils.PROVIDER_URI_LOCK_MAGAZINE_DEFAULT;
    private static String sWallpaperAuthority = "com.miui.home.none_provider";

    public static void setWallpaperAuthority(String authority) {
        sWallpaperAuthority = authority;
    }

    public static String getWallpaperAuthority() {
        return sWallpaperAuthority;
    }

    public static String getWallpaperAuthoritySystemSetting(Context context) {
        String authority = Settings.System.getStringForUser(context.getContentResolver(), "lock_wallpaper_provider_authority", KeyguardUpdateMonitor.getCurrentUser());
        return TextUtils.isEmpty(authority) ? "com.miui.home.none_provider" : authority;
    }

    private static boolean supportVideo24Wallpaper() {
        return "cepheus".equals(Build.DEVICE) || "grus".equals(Build.DEVICE) || "dipper".equals(Build.DEVICE) || "beryllium".equals(Build.DEVICE) || "equuleus".equals(Build.DEVICE) || "perseus".equals(Build.DEVICE) || "andromeda".equals(Build.DEVICE) || "ursa".equals(Build.DEVICE) || "polaris".equals(Build.DEVICE) || "davinci".equals(Build.DEVICE) || "raphael".equals(Build.DEVICE);
    }

    public static boolean isVideo24Wallpaper(Context context) {
        if (supportVideo24Wallpaper() && isHomeDefaultWallpaper()) {
            WallpaperInfo paperInfo = WallpaperManagerCompat.getWallpaperInfo(context);
            if (paperInfo != null && "com.android.systemui.wallpaper.Video24WallpaperService".equals(paperInfo.getServiceName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMiWallpaper(Context context) {
        WallpaperInfo paperInfo = WallpaperManagerCompat.getWallpaperInfo(context);
        if (paperInfo == null || !"com.miui.miwallpaper.MiWallpaper".equals(paperInfo.getServiceName())) {
            return false;
        }
        return true;
    }

    public static void setWallpaperAuthoritySystemSetting(Context context, String authority) {
        Settings.System.putStringForUser(context.getContentResolver(), "lock_wallpaper_provider_authority", authority, KeyguardUpdateMonitor.getCurrentUser());
    }

    public static boolean isThemeLockLiveWallpaper(Context context) {
        if (isVideo24Wallpaper(context)) {
            return true;
        }
        return "com.android.thememanager.theme_lock_live_wallpaper".equals(sWallpaperAuthority);
    }

    public static boolean isThemeLockVideoWallpaper() {
        return "com.android.thememanager.theme_lock_video_wallpaper".equals(sWallpaperAuthority);
    }

    public static boolean isHomeDefaultWallpaper() {
        return "com.miui.home.none_provider".equals(sWallpaperAuthority);
    }

    public static boolean isLockScreenMagazineWallpaper() {
        return isLockScreenMagazineOpenedWallpaper() || isLockScreenMagazineClosedWallpaper();
    }

    public static boolean isLockScreenMagazineOpenedWallpaper() {
        return APPLY_MAGAZINE_DEFAULT_AUTHORITY.equals(sWallpaperAuthority);
    }

    public static boolean isLockScreenMagazineClosedWallpaper() {
        return "com.xiaomi.tv.gallerylockscreen.set_lockwallpaper".equals(sWallpaperAuthority);
    }

    public static boolean isThemeLockWallpaper() {
        return "com.android.thememanager.theme_lockwallpaper".equals(sWallpaperAuthority);
    }

    public static boolean isCustomWallpaper() {
        return "com.android.thememanager.set_lockwallpaper".equals(sWallpaperAuthority) || "com.android.thememanager.theme_lock_live_wallpaper".equals(sWallpaperAuthority) || "com.android.thememanager.theme_lock_video_wallpaper".equals(sWallpaperAuthority);
    }
}
