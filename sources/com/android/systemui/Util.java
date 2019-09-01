package com.android.systemui;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.MiuiConfiguration;
import android.graphics.Outline;
import android.graphics.Rect;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Toast;
import com.android.systemui.miui.ActivityObserver;
import com.android.systemui.miui.ToastOverlayManager;
import java.util.List;

public class Util {
    private static boolean sMiuiOptimizationDisabled;
    private static boolean sUserExperienceEnable;

    public static PackageManager getPackageManagerForUser(Context context, int userId) {
        Context contextForUser = context;
        if (userId >= 0) {
            try {
                contextForUser = context.createPackageContextAsUser(context.getPackageName(), 4, new UserHandle(userId));
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return contextForUser.getPackageManager();
    }

    public static ApplicationInfo getApplicationInfo(Context context, String pkgName, int userId) {
        try {
            return getPackageManagerForUser(context, userId).getApplicationInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getTopActivityPkg(Context context, boolean considerLockscreen) {
        if (considerLockscreen && ((KeyguardManager) context.getSystemService("keyguard")).isKeyguardLocked()) {
            return "lockscreen";
        }
        ComponentName topActivity = getTopActivity(context);
        return topActivity == null ? "" : topActivity.getPackageName();
    }

    public static String getTopActivityPkg(Context context) {
        return getTopActivityPkg(context, false);
    }

    public static ComponentName getTopActivity(Context context) {
        if (isMainProcess()) {
            return ((ActivityObserver) Dependency.get(ActivityObserver.class)).getTopActivity();
        }
        return getTopActivityLegacy(context);
    }

    private static ComponentName getTopActivityLegacy(Context context) {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1);
        if (runningTasks == null || runningTasks.isEmpty()) {
            return null;
        }
        return runningTasks.get(0).topActivity;
    }

    public static boolean isProviderAccess(String authority, int userId) {
        boolean z = false;
        try {
            if (ActivityThread.getPackageManager().resolveContentProvider(authority, 790016, userId) != null) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setUserExperienceProgramEnabled(boolean enable) {
        sUserExperienceEnable = enable;
    }

    public static boolean isUserExperienceProgramEnable() {
        return sUserExperienceEnable;
    }

    public static void setMiuiOptimizationDisabled(boolean miuiOptimizationDisabled) {
        sMiuiOptimizationDisabled = miuiOptimizationDisabled;
    }

    public static boolean isMiuiOptimizationDisabled() {
        return sMiuiOptimizationDisabled;
    }

    public static boolean showCtsSpecifiedColor() {
        return sMiuiOptimizationDisabled && Build.VERSION.SDK_INT > 25;
    }

    public static boolean isGlobalFileExplorerExist(Context context) {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setPackage("com.mi.android.globalFileexplorer");
        return isIntentActivityExist(context, intent);
    }

    public static boolean isCNFileExplorerExist(Context context) {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setPackage("com.android.fileexplorer");
        return isIntentActivityExist(context, intent);
    }

    public static boolean isBrowserSearchExist(Context context) {
        Intent intent = new Intent("com.android.browser.browser_search");
        intent.setPackage("com.android.browser");
        return isIntentActivityExist(context, intent);
    }

    private static boolean isIntentActivityExist(Context context, Intent intent) {
        boolean z = false;
        if (context == null || intent == null) {
            return false;
        }
        try {
            List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 786432);
            if (resolveInfos != null && resolveInfos.size() > 0) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void hideSystemBars(View decorView) {
        decorView.setSystemUiVisibility(12038);
    }

    public static void setViewRoundCorner(View imageView, final float radius) {
        imageView.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(new Rect(0, 0, view.getWidth(), view.getHeight()), radius);
            }
        });
        imageView.setClipToOutline(true);
    }

    public static boolean isDefaultTheme() {
        return !Constants.THEME_FILE.exists();
    }

    public static void playRingtoneAsync(final Context context, final Uri uri, final int streamType) {
        ((UiOffloadThread) Dependency.get(UiOffloadThread.class)).submit(new Runnable() {
            public void run() {
                try {
                    Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
                    if (ringtone != null) {
                        if (streamType >= 0) {
                            ringtone.setStreamType(streamType);
                        }
                        ringtone.play();
                    }
                } catch (Exception e) {
                    Log.e("Util", "error playing ringtone " + uri, e);
                }
            }
        });
    }

    @SuppressLint({"ShowToast"})
    private static Toast makeSystemOverlayToast(Context context, String text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.setType(2006);
        toast.getWindowParams().privateFlags |= 16;
        return toast;
    }

    public static Toast showSystemOverlayToast(Context context, int resId, int duration) {
        return showSystemOverlayToast(context, context.getString(resId), duration);
    }

    public static Toast showSystemOverlayToast(Context context, String text, int duration) {
        Toast toast = makeSystemOverlayToast(context, text, duration);
        toast.show();
        ((ToastOverlayManager) Dependency.get(ToastOverlayManager.class)).dispatchShowToast(toast);
        return toast;
    }

    public static boolean isThemeResourcesChanged(int changes, long themeChangedFlags) {
        boolean miuiThemeChanged = (Integer.MIN_VALUE & changes) != 0 && MiuiConfiguration.needRestartStatusBar(themeChangedFlags);
        boolean uiModeChanged = (changes & 512) != 0;
        if (miuiThemeChanged || uiModeChanged) {
            return true;
        }
        return false;
    }

    public static boolean isMainProcess() {
        String processName = ActivityThread.currentProcessName();
        return !TextUtils.isEmpty(processName) && TextUtils.indexOf(processName, ':') < 0;
    }
}
