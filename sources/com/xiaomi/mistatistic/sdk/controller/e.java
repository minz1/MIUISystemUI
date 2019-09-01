package com.xiaomi.mistatistic.sdk.controller;

import android.annotation.TargetApi;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import com.xiaomi.mistatistic.sdk.controller.d;
import java.util.UUID;

/* compiled from: DeviceIdHolder */
public class e {
    private static final Object a = new Object();
    private static final Object b = new Object();
    /* access modifiers changed from: private */
    public static String c = null;
    private static String d = null;
    private static String e = null;

    /* compiled from: DeviceIdHolder */
    private static class a implements d.a {
        private Context a;

        public a(Context context) {
            this.a = context;
        }

        public void a() {
            if (k.c(this.a, "imei")) {
                k.d(this.a, "imei");
            }
            String a2 = k.a(this.a, "device_id", "");
            if (TextUtils.isEmpty(a2)) {
                String unused = e.c = e.e(this.a);
            } else {
                String unused2 = e.c = a2;
            }
        }
    }

    public String a() {
        if (c != null) {
            return c;
        }
        d.a().a((d.a) new a(c.a()));
        return null;
    }

    /* access modifiers changed from: private */
    public static String e(Context context) {
        if (TextUtils.isEmpty(c)) {
            synchronized (a) {
                if (TextUtils.isEmpty(c)) {
                    try {
                        Context applicationContext = context.getApplicationContext();
                        String a2 = k.a(applicationContext, "device_id", "");
                        if (TextUtils.isEmpty(a2)) {
                            if (!BuildSetting.isInternationalBuild()) {
                                if (!q.c()) {
                                    String c2 = q.c(applicationContext);
                                    String a3 = q.a();
                                    c = q.c(f(context) + c2 + a3);
                                    k.b(applicationContext, "device_id", c);
                                }
                            }
                            String b2 = b(context);
                            if (!TextUtils.isEmpty(b2)) {
                                c = b2;
                            } else {
                                c = c(context);
                            }
                            k.b(applicationContext, "device_id", c);
                        } else {
                            c = a2;
                        }
                    } catch (Exception e2) {
                        h.a("DIH", "getDeviceId exception", (Throwable) e2);
                    }
                }
            }
        }
        return c;
    }

    @TargetApi(9)
    private static String f(Context context) {
        if (TextUtils.isEmpty(d)) {
            synchronized (b) {
                if (TextUtils.isEmpty(d)) {
                    try {
                        if (context.getPackageManager().checkPermission("android.permission.READ_PHONE_STATE", context.getPackageName()) == 0) {
                            d = ((TelephonyManager) context.getSystemService("phone")).getDeviceId();
                        } else {
                            h.d("DIH", "cannot get READ_PHONE_STATE permission");
                        }
                    } catch (Exception e2) {
                        h.a("DIH", "getImei exception:", (Throwable) e2);
                    }
                }
            }
        }
        if (TextUtils.isEmpty(d)) {
            h.c("DIH", "Imei is empty");
        }
        return d;
    }

    public static String b(Context context) {
        String str;
        String a2 = k.a(context, "imei_md5", "");
        if (!TextUtils.isEmpty(a2)) {
            return a2;
        }
        String f = f(context);
        if (TextUtils.isEmpty(f)) {
            str = "";
        } else {
            String b2 = q.b(f);
            if (!TextUtils.isEmpty(b2)) {
                k.b(context, "imei_md5", b2);
            }
            str = b2;
        }
        return str;
    }

    public static synchronized String c(Context context) {
        synchronized (e.class) {
            if (!TextUtils.isEmpty(e)) {
                String str = e;
                return str;
            }
            long currentTimeMillis = System.currentTimeMillis();
            String a2 = k.a(context, "anonymous_id", "");
            long a3 = k.a(context, "aigt", 0);
            long a4 = k.a(context, "anonymous_ei", 7776000000L);
            if (!TextUtils.isEmpty(a2)) {
                if (currentTimeMillis - a3 < a4) {
                    e = a2;
                    k.b(context, "aigt", currentTimeMillis);
                    String str2 = e;
                    return str2;
                }
            }
            e = UUID.randomUUID().toString();
            k.b(context, "anonymous_id", e);
            k.b(context, "aigt", currentTimeMillis);
            String str22 = e;
            return str22;
        }
    }
}
