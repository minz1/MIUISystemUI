package com.android.keyguard.magazine;

import android.app.ActivityManagerCompat;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.util.Log;
import com.android.keyguard.KeyguardCompatibilityHelperForN;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.utils.ContentProviderUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Constants;
import miui.os.Build;

public class LockScreenMagazineUtils {
    public static final String CONTENT_URI_LOCK_MAGAZINE_DEFAULT = ("content://" + PROVIDER_URI_LOCK_MAGAZINE_DEFAULT);
    public static final String LOCK_SCREEN_MAGAZINE_PACKAGE_NAME = (Constants.IS_INTERNATIONAL ? "com.miui.android.fashiongallery" : "com.mfashiongallery.emag");
    public static final String PROVIDER_URI_LOCK_MAGAZINE_DEFAULT = (Build.IS_INTERNATIONAL_BUILD ? "com.miui.android.fashiongallery.lockscreen_magazine_provider" : "com.xiaomi.tv.gallerylockscreen.lockscreen_magazine_provider");
    public static String SYSTEM_SETTINGS_KEY_LOCKSCREEN_MAGAZINE_STATUS = "lock_screen_magazine_status";
    private static boolean sLockScreenMagazinePreviewAvailable = false;

    public static void gotoLockScreenMagazine(Context context, String source) {
        hideLockScreenInActivityManager();
        try {
            Intent intent = new Intent("android.miui.REQUEST_LOCKSCREEN_WALLPAPER");
            intent.putExtra("showTime", System.currentTimeMillis() + 600);
            intent.putExtra("startTime", System.currentTimeMillis());
            intent.putExtra("from", source);
            context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            AnalyticsHelper.record("keyguard_goto_lockscreen_magazine_fail");
            Log.e("LockScreenMagazineUtils", e.toString());
        }
    }

    private static void hideLockScreenInActivityManager() {
        if (Build.VERSION.SDK_INT < 26) {
            try {
                ActivityManagerCompat.setLockScreenShown(false, true);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean getLockScreenMagazineStatus(Context context) {
        return MiuiSettings.Secure.getBoolean(context.getContentResolver(), SYSTEM_SETTINGS_KEY_LOCKSCREEN_MAGAZINE_STATUS, false);
    }

    public static void setLockScreenMagazineStatus(Context context, boolean status) {
        MiuiSettings.Secure.putBoolean(context.getContentResolver(), SYSTEM_SETTINGS_KEY_LOCKSCREEN_MAGAZINE_STATUS, status);
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v3, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v4, resolved type: com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo getLockScreenMagazineWallpaperInfo(android.content.Context r6) {
        /*
            r0 = 0
            r1 = 0
            r2 = r1
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x002e }
            r3.<init>()     // Catch:{ Exception -> 0x002e }
            java.lang.String r4 = "content://"
            r3.append(r4)     // Catch:{ Exception -> 0x002e }
            java.lang.String r4 = com.android.keyguard.utils.HomeUtils.HOME_LAUNCHER_SETTINGS_AUTHORITY     // Catch:{ Exception -> 0x002e }
            r3.append(r4)     // Catch:{ Exception -> 0x002e }
            java.lang.String r3 = r3.toString()     // Catch:{ Exception -> 0x002e }
            android.net.Uri r3 = android.net.Uri.parse(r3)     // Catch:{ Exception -> 0x002e }
            int r4 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()     // Catch:{ Exception -> 0x002e }
            android.net.Uri r3 = com.android.keyguard.MiuiKeyguardUtils.maybeAddUserId(r3, r4)     // Catch:{ Exception -> 0x002e }
            java.lang.String r4 = r3.toSafeString()     // Catch:{ Exception -> 0x002e }
            java.lang.String r5 = "getLockWallpaperInfo"
            android.os.Bundle r1 = com.android.keyguard.utils.ContentProviderUtils.getResultFromProvider((android.content.Context) r6, (java.lang.String) r4, (java.lang.String) r5, (java.lang.String) r1, (android.os.Bundle) r1)     // Catch:{ Exception -> 0x002e }
            r2 = r1
            goto L_0x0047
        L_0x002e:
            r1 = move-exception
            java.lang.String r3 = "LockScreenMagazineUtils"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "queryLockScreenMagazineWallpaperInfo exception uri = "
            r4.append(r5)
            java.lang.String r5 = com.android.keyguard.utils.HomeUtils.HOME_LAUNCHER_SETTINGS_AUTHORITY
            r4.append(r5)
            java.lang.String r4 = r4.toString()
            android.util.Log.e(r3, r4, r1)
        L_0x0047:
            if (r2 == 0) goto L_0x0079
            com.google.gson.Gson r1 = new com.google.gson.Gson
            r1.<init>()
            java.lang.String r3 = "LockScreenMagazineUtils"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "queryLockScreenMagazineWallpaperInfo bundlestring="
            r4.append(r5)
            java.lang.String r5 = "result_json"
            java.lang.String r5 = r2.getString(r5)
            r4.append(r5)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r3, r4)
            java.lang.String r3 = "result_json"
            java.lang.String r3 = r2.getString(r3)
            java.lang.Class<com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo> r4 = com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo.class
            java.lang.Object r3 = r1.fromJson(r3, r4)
            r0 = r3
            com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo r0 = (com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo) r0
        L_0x0079:
            if (r0 != 0) goto L_0x0081
            com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo r1 = new com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo
            r1.<init>()
            r0 = r1
        L_0x0081:
            r0.initExtra()
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.magazine.LockScreenMagazineUtils.getLockScreenMagazineWallpaperInfo(android.content.Context):com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo");
    }

    public static Bundle getLockScreenMagazinePreContent(Context context) {
        return ContentProviderUtils.getResultFromProvider(context, CONTENT_URI_LOCK_MAGAZINE_DEFAULT, "getTransitionInfo", (String) null, (Bundle) null);
    }

    public static String getLockScreenMagazineSettingsDeepLink(Context context) {
        Bundle bundle = ContentProviderUtils.getResultFromProvider(context, CONTENT_URI_LOCK_MAGAZINE_DEFAULT, "getAppSettingsDeeplink", (String) null, (Bundle) null);
        if (bundle != null) {
            return bundle.getString("result_string");
        }
        return null;
    }

    public static void notifySubscriptionChange(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            /* access modifiers changed from: protected */
            public Void doInBackground(Void... params) {
                ContentProviderUtils.getResultFromProvider(context, LockScreenMagazineUtils.CONTENT_URI_LOCK_MAGAZINE_DEFAULT, "subscriptionChange", (String) null, (Bundle) null);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public static boolean supportLockScreenMagazineRegion(Context context) {
        return !miui.os.Build.IS_INTERNATIONAL_BUILD || MiuiKeyguardUtils.isIndianRegion(context);
    }

    public static boolean isLockScreenMagazineAvailable(Context context) {
        return !Constants.IS_TABLET && KeyguardCompatibilityHelperForN.isUserUnlocked(context) && MiuiKeyguardUtils.isDefaultLockScreenTheme() && supportLockScreenMagazineRegion(context);
    }

    public static void sendLockScreenMagazineEventBrodcast(Context context, String event) {
        if (miui.os.Build.IS_INTERNATIONAL_BUILD && KeyguardUpdateMonitor.getInstance(context).isSupportLockScreenMagazineLeft() && WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper()) {
            String wallpaperUri = KeyguardUpdateMonitor.getInstance(context).getLockScreenMagazineWallpaperInfo().wallpaperUri;
            Intent intent = new Intent("com.miui.systemui.LOCKSCREEN_WALLPAPER_EVENTS");
            intent.putExtra("wallpaper_uri", wallpaperUri);
            intent.putExtra("wallpaper_view_event", event);
            intent.setPackage(LOCK_SCREEN_MAGAZINE_PACKAGE_NAME);
            context.sendBroadcast(intent);
        }
    }
}
