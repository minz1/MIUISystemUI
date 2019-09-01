package com.xiaomi.mistatistic.sdk.controller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import com.xiaomi.mistatistic.sdk.CustomSettings;
import com.xiaomi.mistatistic.sdk.controller.d;
import com.xiaomi.mistatistic.sdk.data.AbstractEvent;
import com.xiaomi.mistatistic.sdk.data.StatEventPojo;
import com.xiaomi.mistatistic.sdk.data.g;
import com.xiaomi.mistatistic.sdk.data.h;
import com.xiaomi.xmsf.push.service.b;
import java.util.ArrayList;
import java.util.List;

public abstract class LocalEventRecorder {
    /* access modifiers changed from: private */
    public static volatile b a = null;
    /* access modifiers changed from: private */
    public static volatile boolean b = false;
    /* access modifiers changed from: private */
    public static List<AbstractEvent> c = new ArrayList();
    /* access modifiers changed from: private */
    public static Object d = new Object();
    /* access modifiers changed from: private */
    public static ServiceConnection e = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName componentName) {
            h.a("LER", "IStatService has unexpectedly disconnected");
            b unused = LocalEventRecorder.a = null;
            boolean unused2 = LocalEventRecorder.b = false;
        }

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            h.a("LER", "IStatService connected");
            b unused = LocalEventRecorder.a = b.a.a(iBinder);
            if (LocalEventRecorder.a != null) {
                d.a().a((d.a) new d.a() {
                    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0063, code lost:
                        r1 = move-exception;
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0064, code lost:
                        r2 = r1;
                        r1 = r3;
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:20:0x008a, code lost:
                        r1 = move-exception;
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:21:0x008c, code lost:
                        r1 = move-exception;
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:24:?, code lost:
                        com.xiaomi.mistatistic.sdk.controller.h.a("dispatch event to IStatService exception", (java.lang.Throwable) r1);
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:27:?, code lost:
                        com.xiaomi.mistatistic.sdk.controller.h.a("LER", "pending eventList size: " + com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.e().size());
                        r1 = com.xiaomi.mistatistic.sdk.controller.c.a();
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00ea, code lost:
                        com.xiaomi.mistatistic.sdk.controller.h.a("LER", "pending eventList size: " + com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.e().size());
                        com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.a(com.xiaomi.mistatistic.sdk.controller.c.a());
                     */
                    /* JADX WARNING: Code restructure failed: missing block: B:39:0x010f, code lost:
                        throw r1;
                     */
                    /* JADX WARNING: Failed to process nested try/catch */
                    /* JADX WARNING: Removed duplicated region for block: B:21:0x008c A[ExcHandler: RemoteException (r1v6 'e' android.os.RemoteException A[CUSTOM_DECLARE]), Splitter:B:3:0x0008] */
                    /* Code decompiled incorrectly, please refer to instructions dump. */
                    public void a() {
                        /*
                            r5 = this;
                            java.lang.Object r0 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.d
                            monitor-enter(r0)
                            r1 = 0
                            java.lang.String r2 = "LER"
                            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            r3.<init>()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            java.lang.String r4 = "start insert event to IStatService and eventList size: "
                            r3.append(r4)     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            java.util.List r4 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            int r4 = r4.size()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            r3.append(r4)     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            java.lang.String r3 = r3.toString()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r2, (java.lang.String) r3)     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            java.util.List r2 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            if (r2 == 0) goto L_0x0067
                            java.util.List r2 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            boolean r2 = r2.isEmpty()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            if (r2 != 0) goto L_0x0067
                            java.util.List r2 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            java.util.Iterator r2 = r2.iterator()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                        L_0x003c:
                            boolean r3 = r2.hasNext()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            if (r3 == 0) goto L_0x0067
                            java.lang.Object r3 = r2.next()     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            com.xiaomi.mistatistic.sdk.data.AbstractEvent r3 = (com.xiaomi.mistatistic.sdk.data.AbstractEvent) r3     // Catch:{ JSONException -> 0x00b8, RemoteException -> 0x008c }
                            com.xiaomi.xmsf.push.service.b r1 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.a     // Catch:{ JSONException -> 0x0063, RemoteException -> 0x008c }
                            org.json.JSONObject r4 = r3.valueToJSon()     // Catch:{ JSONException -> 0x0063, RemoteException -> 0x008c }
                            java.lang.String r4 = r4.toString()     // Catch:{ JSONException -> 0x0063, RemoteException -> 0x008c }
                            r1.a(r4)     // Catch:{ JSONException -> 0x0063, RemoteException -> 0x008c }
                            java.lang.String r1 = "LER"
                            java.lang.String r4 = "insert a reserved event into IStatService"
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.String) r4)     // Catch:{ JSONException -> 0x0063, RemoteException -> 0x008c }
                            r2.remove()     // Catch:{ JSONException -> 0x0063, RemoteException -> 0x008c }
                            r1 = r3
                            goto L_0x003c
                        L_0x0063:
                            r1 = move-exception
                            r2 = r1
                            r1 = r3
                            goto L_0x00b9
                        L_0x0067:
                            java.lang.String r1 = "LER"
                            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0110 }
                            r2.<init>()     // Catch:{ all -> 0x0110 }
                            java.lang.String r3 = "pending eventList size: "
                            r2.append(r3)     // Catch:{ all -> 0x0110 }
                            java.util.List r3 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ all -> 0x0110 }
                            int r3 = r3.size()     // Catch:{ all -> 0x0110 }
                            r2.append(r3)     // Catch:{ all -> 0x0110 }
                            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0110 }
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.String) r2)     // Catch:{ all -> 0x0110 }
                            android.content.Context r1 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ all -> 0x0110 }
                            goto L_0x00b4
                        L_0x008a:
                            r1 = move-exception
                            goto L_0x00ea
                        L_0x008c:
                            r1 = move-exception
                            java.lang.String r2 = "dispatch event to IStatService exception"
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r2, (java.lang.Throwable) r1)     // Catch:{ all -> 0x008a }
                            java.lang.String r1 = "LER"
                            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0110 }
                            r2.<init>()     // Catch:{ all -> 0x0110 }
                            java.lang.String r3 = "pending eventList size: "
                            r2.append(r3)     // Catch:{ all -> 0x0110 }
                            java.util.List r3 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ all -> 0x0110 }
                            int r3 = r3.size()     // Catch:{ all -> 0x0110 }
                            r2.append(r3)     // Catch:{ all -> 0x0110 }
                            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0110 }
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.String) r2)     // Catch:{ all -> 0x0110 }
                            android.content.Context r1 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ all -> 0x0110 }
                        L_0x00b4:
                            com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c(r1)     // Catch:{ all -> 0x0110 }
                            goto L_0x00e8
                        L_0x00b8:
                            r2 = move-exception
                        L_0x00b9:
                            java.lang.String r3 = "dispatch event to IStatService exception"
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r3, (java.lang.Throwable) r2)     // Catch:{ all -> 0x008a }
                            java.util.List r2 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ all -> 0x008a }
                            r2.remove(r1)     // Catch:{ all -> 0x008a }
                            java.lang.String r1 = "LER"
                            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0110 }
                            r2.<init>()     // Catch:{ all -> 0x0110 }
                            java.lang.String r3 = "pending eventList size: "
                            r2.append(r3)     // Catch:{ all -> 0x0110 }
                            java.util.List r3 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ all -> 0x0110 }
                            int r3 = r3.size()     // Catch:{ all -> 0x0110 }
                            r2.append(r3)     // Catch:{ all -> 0x0110 }
                            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0110 }
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.String) r2)     // Catch:{ all -> 0x0110 }
                            android.content.Context r1 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ all -> 0x0110 }
                            goto L_0x00b4
                        L_0x00e8:
                            monitor-exit(r0)     // Catch:{ all -> 0x0110 }
                            return
                        L_0x00ea:
                            java.lang.String r2 = "LER"
                            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x0110 }
                            r3.<init>()     // Catch:{ all -> 0x0110 }
                            java.lang.String r4 = "pending eventList size: "
                            r3.append(r4)     // Catch:{ all -> 0x0110 }
                            java.util.List r4 = com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c     // Catch:{ all -> 0x0110 }
                            int r4 = r4.size()     // Catch:{ all -> 0x0110 }
                            r3.append(r4)     // Catch:{ all -> 0x0110 }
                            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x0110 }
                            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r2, (java.lang.String) r3)     // Catch:{ all -> 0x0110 }
                            android.content.Context r2 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ all -> 0x0110 }
                            com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.c(r2)     // Catch:{ all -> 0x0110 }
                            throw r1     // Catch:{ all -> 0x0110 }
                        L_0x0110:
                            r1 = move-exception
                            monitor-exit(r0)     // Catch:{ all -> 0x0110 }
                            throw r1
                        */
                        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder.AnonymousClass3.AnonymousClass1.a():void");
                    }
                });
            }
        }
    };

    private static class a implements d.a {
        private AbstractEvent a;

        public a(AbstractEvent abstractEvent) {
            this.a = abstractEvent;
        }

        public void a() {
            StatEventPojo pojo = this.a.toPojo();
            f fVar = new f();
            if ((this.a instanceof g) || (this.a instanceof h)) {
                String str = pojo.key;
                String str2 = pojo.category;
                StatEventPojo a2 = fVar.a(str2, str);
                if (a2 == null || !pojo.type.equals(a2.type)) {
                    fVar.a(pojo);
                } else {
                    fVar.a(str, str2, pojo.value);
                }
            } else {
                fVar.a(pojo);
            }
        }
    }

    public static void insertEvent(final AbstractEvent abstractEvent) {
        Context a2 = c.a();
        if (a2 == null) {
            h.a("LER", "mistats is not initialized properly.");
        } else if (BuildSetting.isCTABuild()) {
            h.a("LER", "disable local event upload for CTA build");
        } else if (CustomSettings.isUseSystemStatService()) {
            h.b("LER", "insert event use systemstatsvc");
            d.a().a((d.a) new d.a() {
                public void a() {
                    LocalEventRecorder.b(abstractEvent);
                }
            });
        } else if (!BuildSetting.isDisabled(a2) || a(abstractEvent.getCategory()) || abstractEvent.getAnonymous() != 0) {
            d.a().a((d.a) new a(abstractEvent));
            p.a().c();
        } else {
            h.a("LER", "disabled local event upload, event category:" + abstractEvent.getCategory());
        }
    }

    private static boolean a(String str) {
        return "mistat_basic".equals(str) || "mistat_session".equals(str) || "mistat_pt".equals(str) || "mistat_pv".equals(str) || "mistat_session_extra".equals(str);
    }

    /* access modifiers changed from: private */
    public static void b(AbstractEvent abstractEvent) {
        try {
            b(c.a());
            if (a != null) {
                a.a(abstractEvent.valueToJSon().toString());
            } else {
                synchronized (d) {
                    c.add(abstractEvent);
                }
            }
        } catch (Throwable th) {
            h.a("LER", "insertEventUseSystemService exception: ", th);
        }
    }

    private static void b(Context context) throws InterruptedException {
        if (!b) {
            Intent intent = new Intent();
            intent.setClassName("com.xiaomi.xmsf", "com.xiaomi.xmsf.push.service.StatService");
            b = context.bindService(intent, e, 1);
            h.a("LER", "bind StatSystemService: " + b);
        }
    }

    /* access modifiers changed from: private */
    public static void c(final Context context) {
        d.a().a((d.a) new d.a() {
            public void a() {
                try {
                    if (LocalEventRecorder.b) {
                        context.unbindService(LocalEventRecorder.e);
                        boolean unused = LocalEventRecorder.b = false;
                        b unused2 = LocalEventRecorder.a = null;
                        h.a("LER", "unbind StatSystemService success");
                        return;
                    }
                    h.a("LER", "StatSystemService is already disconnected");
                } catch (Exception e) {
                    h.a("", (Throwable) e);
                }
            }
        }, 180000);
    }
}
