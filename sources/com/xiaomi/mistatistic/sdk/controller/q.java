package com.xiaomi.mistatistic.sdk.controller;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

/* compiled from: Utils */
public class q {
    private static boolean a = true;

    public static byte[] a(String str) {
        if (str == null) {
            return new byte[0];
        }
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return str.getBytes();
        }
    }

    public static String b(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(a(str));
            return String.format("%1$032X", new Object[]{new BigInteger(1, instance.digest())});
        } catch (NoSuchAlgorithmException e) {
            return str;
        }
    }

    public static String c(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA1");
            instance.update(a(str));
            return String.format("%1$032X", new Object[]{new BigInteger(1, instance.digest())});
        } catch (NoSuchAlgorithmException e) {
            return str;
        }
    }

    public static boolean a(Context context) {
        boolean z;
        try {
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
            if (runningAppProcesses == null || runningAppProcesses.isEmpty()) {
                z = false;
            } else {
                z = false;
                for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
                    try {
                        if (next.importance == 100 || next.importance == 125) {
                            String[] strArr = next.pkgList;
                            int length = strArr.length;
                            boolean z2 = z;
                            int i = 0;
                            while (i < length) {
                                try {
                                    String str = strArr[i];
                                    if (str.equals(context.getPackageName())) {
                                        h.b("U", String.format(" %s importance %d", new Object[]{str, Integer.valueOf(next.importance)}));
                                        z2 = true;
                                    }
                                    i++;
                                } catch (Throwable th) {
                                    th = th;
                                    z = z2;
                                    h.a("isForegroundRunning exception ", th);
                                    return z;
                                }
                            }
                            z = z2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
            h.b("U", "%s foreground running %s", context.getPackageName(), Boolean.valueOf(z));
        } catch (Throwable th3) {
            th = th3;
            z = false;
            h.a("isForegroundRunning exception ", th);
            return z;
        }
        return z;
    }

    public static boolean a(long j) {
        Calendar instance = Calendar.getInstance();
        instance.set(11, 0);
        instance.set(12, 0);
        instance.set(13, 0);
        instance.set(14, 0);
        long timeInMillis = instance.getTimeInMillis();
        long j2 = 86400000 + timeInMillis;
        if (timeInMillis > j || j >= j2) {
            return false;
        }
        return true;
    }

    public static boolean a(long j, long j2) {
        return Math.abs(System.currentTimeMillis() - j) > j2;
    }

    private static String d() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface nextElement = networkInterfaces.nextElement();
                byte[] hardwareAddress = nextElement.getHardwareAddress();
                if (hardwareAddress != null) {
                    if (hardwareAddress.length != 0) {
                        StringBuilder sb = new StringBuilder();
                        for (byte valueOf : hardwareAddress) {
                            sb.append(String.format("%02x:", new Object[]{Byte.valueOf(valueOf)}));
                        }
                        if (sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        String sb2 = sb.toString();
                        if ("wlan0".equals(nextElement.getName())) {
                            return sb2;
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0032  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0037 A[RETURN] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String b(android.content.Context r3) {
        /*
            java.lang.String r0 = ""
            int r1 = android.os.Build.VERSION.SDK_INT
            r2 = 23
            if (r1 < r2) goto L_0x000c
            java.lang.String r0 = d()
        L_0x000c:
            boolean r1 = android.text.TextUtils.isEmpty(r0)
            if (r1 == 0) goto L_0x002b
            java.lang.String r1 = "wifi"
            java.lang.Object r3 = r3.getSystemService(r1)     // Catch:{ Exception -> 0x0023 }
            android.net.wifi.WifiManager r3 = (android.net.wifi.WifiManager) r3     // Catch:{ Exception -> 0x0023 }
            android.net.wifi.WifiInfo r3 = r3.getConnectionInfo()     // Catch:{ Exception -> 0x0023 }
            java.lang.String r3 = r3.getMacAddress()     // Catch:{ Exception -> 0x0023 }
            goto L_0x002c
        L_0x0023:
            r3 = move-exception
            java.lang.String r1 = "U"
            java.lang.String r2 = "getMacMd5 exception: "
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.String) r2, (java.lang.Throwable) r3)
        L_0x002b:
            r3 = r0
        L_0x002c:
            boolean r0 = android.text.TextUtils.isEmpty(r3)
            if (r0 != 0) goto L_0x0037
            java.lang.String r3 = b((java.lang.String) r3)
            return r3
        L_0x0037:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.q.b(android.content.Context):java.lang.String");
    }

    public static String c(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(), "android_id");
        } catch (Throwable th) {
            h.a("U", "getAndroidId exception: ", th);
            return null;
        }
    }

    public static String a() {
        if (Build.VERSION.SDK_INT > 8) {
            return Build.SERIAL;
        }
        return null;
    }

    public static boolean d(Context context) {
        if (k.b(context, "is_miui")) {
            return k.a(context, "is_miui");
        }
        boolean z = false;
        try {
            context.getPackageManager().getPackageInfo("com.xiaomi.xmsf", 0);
            z = true;
        } catch (Exception e) {
            h.b("U", "cannot get pkginfo com.xiaomi.xmsf, not miui.", (Throwable) e);
        }
        k.b(context, "is_miui", z);
        return z;
    }

    public static boolean e(Context context) {
        if (k.b(context, "is_xiaomi")) {
            return k.a(context, "is_xiaomi");
        }
        String packageName = context.getPackageName();
        if (packageName.contains("miui") || packageName.contains("xiaomi")) {
            k.b(context, "is_xiaomi", true);
            return true;
        } else if (!d(context)) {
            k.b(context, "is_xiaomi", false);
            return false;
        } else {
            boolean z = (context.getApplicationInfo().flags & 1) != 0;
            h.b("U", "the pkg %s is sys app %s", packageName, Boolean.valueOf(z));
            k.b(context, "is_xiaomi", z);
            return z;
        }
    }

    public static String f(Context context) {
        if (!d(context)) {
            return null;
        }
        try {
            Class<?> cls = Class.forName("miui.telephony.TelephonyManager");
            Method declaredMethod = cls.getDeclaredMethod("getDefault", new Class[0]);
            declaredMethod.setAccessible(true);
            Object invoke = declaredMethod.invoke(null, new Object[0]);
            if (invoke == null) {
                return null;
            }
            Method declaredMethod2 = cls.getDeclaredMethod("getMiuiDeviceId", new Class[0]);
            declaredMethod2.setAccessible(true);
            Object invoke2 = declaredMethod2.invoke(invoke, new Object[0]);
            if (invoke2 == null || !(invoke2 instanceof String)) {
                return null;
            }
            return String.class.cast(invoke2);
        } catch (Exception e) {
            h.a("getMiuiImei exception: ", (Throwable) e);
            return null;
        }
    }

    public static boolean b() {
        return a;
    }

    private static boolean e() {
        try {
            return c.a().getSharedPreferences("mistat_global_pre", 0).getBoolean("enable_global", false);
        } catch (Exception e) {
            h.a("isSelectGlobalUpload exception: ", (Throwable) e);
            return false;
        }
    }

    public static boolean c() {
        try {
            return c.a().getSharedPreferences("mistat_global_pre", 0).getBoolean("non_miui_global_market", false);
        } catch (Exception e) {
            h.a("isNonMiuiGlobalMarket exception: ", (Throwable) e);
            return false;
        }
    }

    public static String a(Context context, String str) {
        String str2;
        Exception e;
        try {
            if ((!d(context) || !BuildSetting.isInternationalBuild()) && !e()) {
                str2 = str;
                return str2;
            }
            if (!str.toLowerCase().startsWith("http")) {
                str2 = "https://" + str;
            } else {
                str2 = str;
            }
            try {
                String host = new URL(str2).getHost();
                String str3 = "";
                if (host.contains(".")) {
                    String[] split = host.split("\\.");
                    if (split != null && split.length > 0) {
                        for (int i = 0; i < split.length; i++) {
                            if (i == split.length - 2) {
                                str3 = str3 + "intl.";
                            }
                            str3 = str3 + split[i];
                            if (i < split.length - 1) {
                                str3 = str3 + ".";
                            }
                        }
                    }
                } else {
                    str3 = "intl." + host;
                }
                return str2.replace(host, str3);
            } catch (Exception e2) {
                e = e2;
                h.a("U", "ensureInternationalServer exception", (Throwable) e);
                return str2;
            }
        } catch (Exception e3) {
            String str4 = str;
            e = e3;
            str2 = str4;
            h.a("U", "ensureInternationalServer exception", (Throwable) e);
            return str2;
        }
    }
}
