package com.xiaomi.mistatistic.sdk.controller;

import android.app.ActivityManager;
import android.os.Build;
import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import com.xiaomi.mistatistic.sdk.CustomSettings;
import com.xiaomi.mistatistic.sdk.controller.asyncjobs.b;
import com.xiaomi.mistatistic.sdk.controller.asyncjobs.c;
import com.xiaomi.mistatistic.sdk.controller.d;
import com.xiaomi.mistatistic.sdk.data.h;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/* compiled from: RemoteDataUploadManager */
public class l {
    /* access modifiers changed from: private */
    public static AtomicBoolean a = new AtomicBoolean(false);

    /* compiled from: RemoteDataUploadManager */
    public class a implements d.a {
        public a() {
        }

        public void a() {
            l.this.a(false);
        }
    }

    public void a() {
        a(true);
    }

    public void a(boolean z) {
        if (!CustomSettings.isDataUploadingEnabled()) {
            h.a("upload is disabled.", (Throwable) null);
        } else if (!j.a(c.a())) {
            h.d("RDUM", "Current network is not connected.");
        } else {
            if (a.compareAndSet(false, true)) {
                if (c()) {
                    e();
                    f fVar = new f();
                    b(false);
                    if (fVar.f()) {
                        b(true);
                    }
                    p.a().d();
                } else {
                    a.set(false);
                    h.d("upload is not allowed by the server. set Uploading " + a.get());
                }
            } else if (z) {
                h.a(String.format("sUploading %s, trigger uploading job with delay %d", new Object[]{Boolean.valueOf(a.get()), 10000L}));
                d.b().a((d.a) new a(), 10000);
            }
        }
    }

    public static boolean b() {
        return a.get();
    }

    private void b(boolean z) {
        d.b().a((d.a) new b(p.a().f(), z, new b.a() {
            public void a(String str, long j, long j2, int i, boolean z) {
                if (!TextUtils.isEmpty(str)) {
                    l.this.a(str, j, j2, i, z);
                    return;
                }
                l.a.set(false);
                h.a("RDUM", "packing completed with empty data, set Uploading " + l.a.get());
            }
        }));
    }

    /* access modifiers changed from: private */
    public void a(String str, long j, long j2, int i, boolean z) {
        final long j3 = j;
        final long j4 = j2;
        AnonymousClass2 r1 = new c.a() {
            public void a(boolean z, boolean z2) {
                if (z) {
                    l.this.a(j3, j4, z2);
                    return;
                }
                l.a.set(false);
                h.a("RDUM", "upload failed, set Uploading " + l.a.get());
            }
        };
        new c(str, r1, i, z).a();
    }

    /* access modifiers changed from: private */
    public void a(long j, long j2, boolean z) {
        try {
            f fVar = new f();
            fVar.a(Boolean.valueOf(z));
            fVar.a(j, j2);
        } catch (Throwable th) {
            h.a("RDUM", "doDeleting exception: ", th);
        }
        a.set(false);
        h.a("RDUM", "delete done, set Uploading " + a.get());
    }

    private void e() {
        d.a().a((d.a) new d.a() {
            public void a() {
                l.this.f();
            }
        });
    }

    /* access modifiers changed from: private */
    public void f() {
        try {
            if (CustomSettings.isUploadForegroundPackageEnabled() && Build.VERSION.SDK_INT <= 21) {
                ArrayList arrayList = new ArrayList();
                List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = ((ActivityManager) c.a().getSystemService("activity")).getRunningAppProcesses();
                if (runningAppProcesses != null) {
                    for (ActivityManager.RunningAppProcessInfo next : runningAppProcesses) {
                        if (next.importance == 100) {
                            arrayList.add(next.processName);
                        }
                    }
                    h hVar = new h("mistat_basic", "foreground_package", TextUtils.join(",", arrayList));
                    if (BuildSetting.isInternationalBuild() || q.c()) {
                        hVar.setAnonymous(1);
                    }
                    LocalEventRecorder.insertEvent(hVar);
                }
            }
        } catch (Throwable th) {
            h.a("", th);
        }
    }

    public static void a(long j) {
        k.b(c.a(), "next_upload_ts", System.currentTimeMillis() + j);
    }

    public static boolean c() {
        return System.currentTimeMillis() > k.a(c.a(), "next_upload_ts", 0);
    }
}
