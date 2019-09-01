package com.xiaomi.mistatistic.sdk.controller;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* compiled from: PrefPersistUtils */
public class k {
    public static String a = null;
    private static ExecutorService b = Executors.newSingleThreadExecutor();

    public static int a(Context context, String str, int i) {
        if (context == null) {
            String e = h.e("PPU");
            Log.w(e, "Context is null, getInt return default value: " + i);
            return i;
        }
        try {
            return context.getSharedPreferences(a(context), 0).getInt(str, i);
        } catch (Exception e2) {
            h.a("PPU", "getInt exception", (Throwable) e2);
            return i;
        }
    }

    public static long a(Context context, String str, long j) {
        if (context == null) {
            String e = h.e("PPU");
            Log.w(e, "Context is null, getLong return default value: " + j);
            return j;
        }
        try {
            return context.getSharedPreferences(a(context), 0).getLong(str, j);
        } catch (Exception e2) {
            h.a("PPU", "getLong exception", (Throwable) e2);
            return j;
        }
    }

    public static String a(Context context, String str, String str2) {
        if (context == null) {
            String e = h.e("PPU");
            Log.w(e, "Context is null, getString return default value: " + str2);
            return str2;
        }
        try {
            return context.getSharedPreferences(a(context), 0).getString(str, str2);
        } catch (Exception e2) {
            h.a("PPU", "getString exception", (Throwable) e2);
            return str2;
        }
    }

    public static boolean a(Context context, String str, boolean z) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, getBoolean return default value: defaultValue.");
            return z;
        }
        try {
            return context.getSharedPreferences(a(context), 0).getBoolean(str, z);
        } catch (Exception e) {
            h.a("PPU", "getBoolean exception", (Throwable) e);
            return z;
        }
    }

    public static boolean a(Context context, String str) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, getBoolean return default value: false.");
            return false;
        }
        try {
            return context.getSharedPreferences(a(context), 0).getBoolean(str, false);
        } catch (Exception e) {
            h.a("PPU", "getBoolean exception", (Throwable) e);
            return false;
        }
    }

    public static void b(Context context, String str, int i) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, putInt do nothing.");
            return;
        }
        try {
            SharedPreferences.Editor edit = context.getSharedPreferences(a(context), 0).edit();
            edit.putInt(str, i);
            a(edit);
        } catch (Exception e) {
            h.a("PPU", "putInt exception", (Throwable) e);
        }
    }

    public static void b(Context context, String str, long j) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, putLong do nothing.");
            return;
        }
        try {
            SharedPreferences.Editor edit = context.getSharedPreferences(a(context), 0).edit();
            edit.putLong(str, j);
            a(edit);
        } catch (Exception e) {
            h.a("PPU", "putLong exception", (Throwable) e);
        }
    }

    public static void b(Context context, String str, String str2) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, putString do nothing.");
            return;
        }
        try {
            SharedPreferences.Editor edit = context.getSharedPreferences(a(context), 0).edit();
            edit.putString(str, str2);
            a(edit);
        } catch (Exception e) {
            h.a("PPU", "putString exception", (Throwable) e);
        }
    }

    public static void b(Context context, String str, boolean z) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, putBoolean do nothing.");
            return;
        }
        try {
            SharedPreferences.Editor edit = context.getSharedPreferences(a(context), 0).edit();
            edit.putBoolean(str, z);
            a(edit);
        } catch (Exception e) {
            h.a("PPU", "putBoolean exception", (Throwable) e);
        }
    }

    public static boolean b(Context context, String str) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, contains return default value: false.");
            return false;
        }
        try {
            return context.getSharedPreferences(a(context), 0).contains(str);
        } catch (Exception e) {
            h.a("PPU", "contains exception", (Throwable) e);
            return false;
        }
    }

    private static void a(SharedPreferences.Editor editor) {
        editor.apply();
    }

    public static boolean c(Context context, String str) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, hasKey return default value: false.");
            return false;
        }
        try {
            return context.getSharedPreferences(a(context), 0).contains(str);
        } catch (Exception e) {
            h.a("PPU", "hasKey exception", (Throwable) e);
            return false;
        }
    }

    public static boolean d(Context context, String str) {
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, removeKey return default value: false.");
            return false;
        }
        try {
            return context.getSharedPreferences(a(context), 0).edit().remove(str).commit();
        } catch (Exception e) {
            h.a("PPU", "removeKey exception", (Throwable) e);
            return false;
        }
    }

    public static String a(Context context) {
        if (!TextUtils.isEmpty(a)) {
            return a;
        }
        if (context == null) {
            Log.w(h.e("PPU"), "Context is null, getPrefName return a empty string ");
            return "";
        }
        String c = c(context);
        if (TextUtils.equals(c, context.getPackageName())) {
            a = "mistat";
        } else {
            a = "mistat" + q.c(c);
        }
        return a;
    }

    private static String c(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        if (activityManager.getRunningAppProcesses() != null) {
            for (ActivityManager.RunningAppProcessInfo next : activityManager.getRunningAppProcesses()) {
                if (next.pid == Process.myPid()) {
                    return next.processName;
                }
            }
        }
        return "";
    }
}
