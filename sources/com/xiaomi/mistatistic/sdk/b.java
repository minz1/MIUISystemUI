package com.xiaomi.mistatistic.sdk;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.StrictMode;
import com.xiaomi.mistatistic.sdk.controller.c;
import com.xiaomi.mistatistic.sdk.controller.e;
import com.xiaomi.mistatistic.sdk.controller.h;
import com.xiaomi.mistatistic.sdk.controller.j;
import com.xiaomi.mistatistic.sdk.controller.k;
import com.xiaomi.mistatistic.sdk.controller.n;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.Thread;
import java.util.TreeMap;

/* compiled from: MIStatsExceptionHandler */
public class b implements Thread.UncaughtExceptionHandler {
    private static int a = 1;
    private static boolean b = false;
    private final Thread.UncaughtExceptionHandler c;

    /* compiled from: MIStatsExceptionHandler */
    public static class a implements Serializable {
        public Throwable a;
        public String b;
        public String c;
        public String d;
        public String e;

        public a(Throwable th) {
            this.a = th;
            this.b = c.e();
            this.c = c.d();
            this.d = c.f();
            this.e = String.valueOf(System.currentTimeMillis());
        }

        public a() {
            this.a = null;
            this.b = c.e();
            this.c = c.d();
            this.d = c.f();
            this.e = null;
        }
    }

    public static void a(boolean z) {
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(defaultUncaughtExceptionHandler instanceof b)) {
            if (z) {
                defaultUncaughtExceptionHandler = null;
            }
            Thread.setDefaultUncaughtExceptionHandler(new b(defaultUncaughtExceptionHandler));
            b = true;
        }
    }

    public static void a(a aVar, boolean z) {
        h.a("uploadException, isManually:" + z);
        if (b) {
            if (aVar == null || aVar.a == null) {
                throw new IllegalArgumentException("the throwable is null.");
            } else if (aVar.a.getStackTrace() != null && aVar.a.getStackTrace().length != 0) {
                if (!BuildSetting.isUploadDebugLogEnable(c.a())) {
                    h.d("not allowed to upload debug or exception log");
                    return;
                }
                StringWriter stringWriter = new StringWriter();
                aVar.a.printStackTrace(new PrintWriter(stringWriter));
                String obj = stringWriter.toString();
                final TreeMap treeMap = new TreeMap();
                treeMap.put("app_id", c.b());
                treeMap.put("app_key", c.c());
                treeMap.put("device_uuid", new e().a());
                treeMap.put("device_os", "Android " + Build.VERSION.SDK_INT);
                treeMap.put("device_model", Build.MODEL);
                treeMap.put("app_version", aVar.b);
                treeMap.put("sdk_version", "1.9.19");
                treeMap.put("app_channel", aVar.c);
                treeMap.put("app_start_time", aVar.d);
                treeMap.put("app_crash_time", aVar.e);
                treeMap.put("crash_exception_type", aVar.a.getClass().getName() + ":" + aVar.a.getMessage());
                treeMap.put("crash_exception_desc", aVar.a instanceof OutOfMemoryError ? "OutOfMemoryError" : obj);
                treeMap.put("crash_callstack", obj);
                if (z) {
                    treeMap.put("manual", "true");
                }
                n.b.execute(new Runnable() {
                    public void run() {
                        try {
                            j.a(BuildSetting.isTest() ? "http://10.99.168.145:8097/micrash" : "https://data.mistat.xiaomi.com/micrash", treeMap, new j.b() {
                                public void a(String str) {
                                    h.a("upload exception result: " + str);
                                }
                            });
                        } catch (IOException e) {
                            h.a("Error to upload the exception ", (Throwable) e);
                        }
                    }
                });
            }
        }
    }

    public b() {
        this.c = null;
    }

    public b(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.c = uncaughtExceptionHandler;
    }

    @SuppressLint({"NewApi"})
    public void uncaughtException(Thread thread, Throwable th) {
        h.a("uncaughtException...");
        if (Build.VERSION.SDK_INT >= 9) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());
        }
        com.xiaomi.mistatistic.sdk.controller.asyncjobs.a.c();
        if (!MiStatInterface.shouldExceptionUploadImmediately()) {
            a(th);
        } else if (!a()) {
            a(new a(th), false);
        } else {
            h.a("crazy crash...");
        }
        if (this.c != null) {
            this.c.uncaughtException(thread, th);
        }
    }

    public boolean a() {
        if (System.currentTimeMillis() - k.a(c.a(), "crash_time", 0) > 300000) {
            k.b(c.a(), "crash_count", 1);
            k.b(c.a(), "crash_time", System.currentTimeMillis());
        } else {
            int a2 = k.a(c.a(), "crash_count", 0);
            if (a2 == 0) {
                k.b(c.a(), "crash_time", System.currentTimeMillis());
            }
            int i = a2 + 1;
            k.b(c.a(), "crash_count", i);
            if (i > 10) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x004a A[SYNTHETIC, Splitter:B:24:0x004a] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void a(java.lang.Throwable r4) {
        /*
            java.util.ArrayList r0 = b()
            com.xiaomi.mistatistic.sdk.b$a r1 = new com.xiaomi.mistatistic.sdk.b$a
            r1.<init>(r4)
            r0.add(r1)
            int r4 = r0.size()
            r1 = 0
            r2 = 5
            if (r4 <= r2) goto L_0x0017
            r0.remove(r1)
        L_0x0017:
            r4 = 0
            android.content.Context r2 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ IOException -> 0x0039 }
            java.lang.String r3 = ".exceptiondetail"
            java.io.FileOutputStream r1 = r2.openFileOutput(r3, r1)     // Catch:{ IOException -> 0x0039 }
            java.io.ObjectOutputStream r2 = new java.io.ObjectOutputStream     // Catch:{ IOException -> 0x0039 }
            r2.<init>(r1)     // Catch:{ IOException -> 0x0039 }
            r2.writeObject(r0)     // Catch:{ IOException -> 0x0033, all -> 0x002f }
            r2.close()     // Catch:{ IOException -> 0x0045 }
            goto L_0x0044
        L_0x002f:
            r4 = move-exception
            r0 = r4
            r4 = r2
            goto L_0x0048
        L_0x0033:
            r4 = move-exception
            r0 = r4
            r4 = r2
            goto L_0x003a
        L_0x0037:
            r0 = move-exception
            goto L_0x0048
        L_0x0039:
            r0 = move-exception
        L_0x003a:
            java.lang.String r1 = ""
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.Throwable) r0)     // Catch:{ all -> 0x0037 }
            if (r4 == 0) goto L_0x0047
            r4.close()     // Catch:{ IOException -> 0x0045 }
        L_0x0044:
            goto L_0x0047
        L_0x0045:
            r4 = move-exception
            goto L_0x0044
        L_0x0047:
            return
        L_0x0048:
            if (r4 == 0) goto L_0x004f
            r4.close()     // Catch:{ IOException -> 0x004e }
            goto L_0x004f
        L_0x004e:
            r4 = move-exception
        L_0x004f:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.b.a(java.lang.Throwable):void");
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x004f A[SYNTHETIC, Splitter:B:26:0x004f] */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0058  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x005e A[SYNTHETIC, Splitter:B:34:0x005e] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.util.ArrayList<com.xiaomi.mistatistic.sdk.b.a> b() {
        /*
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r1 = 0
            android.content.Context r2 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ Exception -> 0x0046 }
            java.io.File r2 = r2.getFilesDir()     // Catch:{ Exception -> 0x0046 }
            if (r2 == 0) goto L_0x003a
            java.io.File r3 = new java.io.File     // Catch:{ Exception -> 0x0046 }
            java.lang.String r4 = ".exceptiondetail"
            r3.<init>(r2, r4)     // Catch:{ Exception -> 0x0046 }
            boolean r2 = r3.isFile()     // Catch:{ Exception -> 0x0046 }
            if (r2 == 0) goto L_0x003a
            java.io.ObjectInputStream r2 = new java.io.ObjectInputStream     // Catch:{ Exception -> 0x0046 }
            java.io.FileInputStream r4 = new java.io.FileInputStream     // Catch:{ Exception -> 0x0046 }
            r4.<init>(r3)     // Catch:{ Exception -> 0x0046 }
            r2.<init>(r4)     // Catch:{ Exception -> 0x0046 }
            java.lang.Object r1 = r2.readObject()     // Catch:{ Exception -> 0x0035, all -> 0x0032 }
            java.util.ArrayList r1 = (java.util.ArrayList) r1     // Catch:{ Exception -> 0x0035, all -> 0x0032 }
            r0 = r1
            r1 = r2
            goto L_0x003a
        L_0x0032:
            r0 = move-exception
            r1 = r2
            goto L_0x005c
        L_0x0035:
            r1 = move-exception
            r5 = r2
            r2 = r1
            r1 = r5
            goto L_0x0047
        L_0x003a:
            if (r1 == 0) goto L_0x0042
            r1.close()     // Catch:{ IOException -> 0x0040 }
        L_0x003f:
            goto L_0x0042
        L_0x0040:
            r1 = move-exception
            goto L_0x003f
        L_0x0042:
            r1 = 0
            goto L_0x0056
        L_0x0044:
            r0 = move-exception
            goto L_0x005c
        L_0x0046:
            r2 = move-exception
        L_0x0047:
            java.lang.String r3 = ""
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r3, (java.lang.Throwable) r2)     // Catch:{ all -> 0x0044 }
            r2 = 1
            if (r1 == 0) goto L_0x0055
            r1.close()     // Catch:{ IOException -> 0x0053 }
        L_0x0052:
            goto L_0x0055
        L_0x0053:
            r1 = move-exception
            goto L_0x0052
        L_0x0055:
            r1 = r2
        L_0x0056:
            if (r1 == 0) goto L_0x005b
            c()
        L_0x005b:
            return r0
        L_0x005c:
            if (r1 == 0) goto L_0x0063
            r1.close()     // Catch:{ IOException -> 0x0062 }
            goto L_0x0063
        L_0x0062:
            r1 = move-exception
        L_0x0063:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.b.b():java.util.ArrayList");
    }

    public static void c() {
        new File(c.a().getFilesDir(), ".exceptiondetail").delete();
    }

    public static void a(int i) {
        a = i;
    }

    public static int d() {
        return a;
    }
}
