package com.xiaomi.mistatistic.sdk.controller;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;
import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import com.xiaomi.mistatistic.sdk.a;
import com.xiaomi.mistatistic.sdk.data.StatEventPojo;
import java.util.ArrayList;
import java.util.List;

/* compiled from: EventDAO */
public class f {
    public static String a = "";
    public static boolean b = false;
    private static i c;
    private boolean d = false;
    /* access modifiers changed from: private */
    public a e = null;
    /* access modifiers changed from: private */
    public boolean f = false;
    private ServiceConnection g = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            boolean unused = f.this.f = true;
            a unused2 = f.this.e = a.C0000a.a(iBinder);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            boolean unused = f.this.f = false;
            a unused2 = f.this.e = null;
        }
    };

    public static void a() {
        c = new i(c.a());
    }

    public void a(Boolean bool) {
        this.d = bool.booleanValue();
    }

    private void g() {
        if (!this.f) {
            try {
                Intent intent = new Intent(c.a(), Class.forName(a));
                c.a().startService(intent);
                if (this.e != null) {
                    h.b("unbind service before bind it again!");
                    c.a().unbindService(this.g);
                }
                c.a().bindService(intent, this.g, 1);
            } catch (Exception e2) {
                h.a("ensureServiceBinded", (Throwable) e2);
            }
        }
    }

    public StatEventPojo a(String str, String str2) {
        if (!b) {
            return b(str, str2);
        }
        g();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            do {
                if (this.f) {
                    if (this.e != null) {
                        StatEventPojo a2 = this.e.a(str, str2);
                        h.b("process query, result is: " + a2);
                        return a2;
                    }
                }
            } while (System.currentTimeMillis() - currentTimeMillis <= 1000);
            return null;
        } catch (Exception e2) {
            h.a("queryCustomEvent", (Throwable) e2);
            return null;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v2, resolved type: com.xiaomi.mistatistic.sdk.data.StatEventPojo} */
    /* JADX WARNING: type inference failed for: r1v0 */
    /* JADX WARNING: type inference failed for: r1v1, types: [android.database.Cursor] */
    /* JADX WARNING: type inference failed for: r1v3 */
    /* JADX WARNING: type inference failed for: r1v4 */
    /* JADX WARNING: type inference failed for: r1v5 */
    /* JADX WARNING: type inference failed for: r1v6 */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0052, code lost:
        r13 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0053, code lost:
        r1 = r12;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x005a, code lost:
        r12 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x0063, code lost:
        throw r12;
     */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:13:0x0034, B:20:0x0042] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0047 A[SYNTHETIC, Splitter:B:23:0x0047] */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0056 A[Catch:{ all -> 0x0052, all -> 0x005a }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.xiaomi.mistatistic.sdk.data.StatEventPojo b(java.lang.String r12, java.lang.String r13) {
        /*
            r11 = this;
            com.xiaomi.mistatistic.sdk.controller.i r0 = c
            monitor-enter(r0)
            r1 = 0
            com.xiaomi.mistatistic.sdk.controller.i r2 = c     // Catch:{ SQLiteException -> 0x003c, all -> 0x003a }
            android.database.sqlite.SQLiteDatabase r3 = r2.getReadableDatabase()     // Catch:{ SQLiteException -> 0x003c, all -> 0x003a }
            java.lang.String r4 = "mistat_event"
            r5 = 0
            java.lang.String r6 = "category=? AND key=?"
            r2 = 2
            java.lang.String[] r7 = new java.lang.String[r2]     // Catch:{ SQLiteException -> 0x003c, all -> 0x003a }
            r2 = 0
            r7[r2] = r12     // Catch:{ SQLiteException -> 0x003c, all -> 0x003a }
            r12 = 1
            r7[r12] = r13     // Catch:{ SQLiteException -> 0x003c, all -> 0x003a }
            r8 = 0
            r9 = 0
            r10 = 0
            android.database.Cursor r12 = r3.query(r4, r5, r6, r7, r8, r9, r10)     // Catch:{ SQLiteException -> 0x003c, all -> 0x003a }
            if (r12 == 0) goto L_0x0032
            boolean r13 = r12.moveToFirst()     // Catch:{ SQLiteException -> 0x0030 }
            if (r13 == 0) goto L_0x0032
            com.xiaomi.mistatistic.sdk.data.StatEventPojo r13 = a((android.database.Cursor) r12)     // Catch:{ SQLiteException -> 0x0030 }
            r1 = r13
            goto L_0x0032
        L_0x0030:
            r13 = move-exception
            goto L_0x003e
        L_0x0032:
            if (r12 == 0) goto L_0x0037
            r12.close()     // Catch:{ all -> 0x005a }
        L_0x0037:
            com.xiaomi.mistatistic.sdk.controller.i r12 = c     // Catch:{ all -> 0x005a }
            goto L_0x004c
        L_0x003a:
            r13 = move-exception
            goto L_0x0054
        L_0x003c:
            r13 = move-exception
            r12 = r1
        L_0x003e:
            java.lang.String r2 = "EventDAO"
            java.lang.String r3 = "queryCustomEvent exception"
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r2, (java.lang.String) r3, (java.lang.Throwable) r13)     // Catch:{ all -> 0x0052 }
            if (r12 == 0) goto L_0x004a
            r12.close()     // Catch:{ all -> 0x005a }
        L_0x004a:
            com.xiaomi.mistatistic.sdk.controller.i r12 = c     // Catch:{ all -> 0x005a }
        L_0x004c:
            r12.close()     // Catch:{ all -> 0x005a }
            monitor-exit(r0)     // Catch:{ all -> 0x005a }
            return r1
        L_0x0052:
            r13 = move-exception
            r1 = r12
        L_0x0054:
            if (r1 == 0) goto L_0x005c
            r1.close()     // Catch:{ all -> 0x005a }
            goto L_0x005c
        L_0x005a:
            r12 = move-exception
            goto L_0x0062
        L_0x005c:
            com.xiaomi.mistatistic.sdk.controller.i r12 = c     // Catch:{ all -> 0x005a }
            r12.close()     // Catch:{ all -> 0x005a }
            throw r13     // Catch:{ all -> 0x005a }
        L_0x0062:
            monitor-exit(r0)     // Catch:{ all -> 0x005a }
            throw r12
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.f.b(java.lang.String, java.lang.String):com.xiaomi.mistatistic.sdk.data.StatEventPojo");
    }

    public void a(StatEventPojo statEventPojo) {
        if (b) {
            try {
                Intent intent = new Intent(c.a(), Class.forName(a));
                intent.putExtra("type", 1);
                intent.putExtra("StatEventPojo", statEventPojo);
                c.a().startService(intent);
            } catch (Exception e2) {
                h.a("insertNewEvent", (Throwable) e2);
            }
        } else {
            b(statEventPojo);
        }
    }

    public void b(StatEventPojo statEventPojo) {
        i iVar;
        ContentValues contentValues = new ContentValues();
        contentValues.put("category", statEventPojo.category);
        contentValues.put("key", TextUtils.isEmpty(statEventPojo.key) ? "" : statEventPojo.key);
        contentValues.put("ts", Long.valueOf(statEventPojo.timeStamp));
        contentValues.put("type", TextUtils.isEmpty(statEventPojo.type) ? "" : statEventPojo.type);
        contentValues.put("value", TextUtils.isEmpty(statEventPojo.value) ? "" : statEventPojo.value);
        contentValues.put("extra", TextUtils.isEmpty(statEventPojo.extra) ? "" : statEventPojo.extra);
        contentValues.put("anonymous", Integer.valueOf(statEventPojo.anonymous));
        synchronized (c) {
            try {
                c.getWritableDatabase().insert("mistat_event", "", contentValues);
                iVar = c;
            } catch (SQLiteException e2) {
                try {
                    h.a("EventDAO", "Error to insert data into DB, key=" + statEventPojo.key, (Throwable) e2);
                    iVar = c;
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
            iVar.close();
        }
    }

    public void a(String str, String str2, String str3) {
        if (b) {
            try {
                Intent intent = new Intent(c.a(), Class.forName(a));
                intent.putExtra("type", 2);
                intent.putExtra("key", str);
                intent.putExtra("category", str2);
                intent.putExtra("newValue", str3);
                c.a().startService(intent);
            } catch (Exception e2) {
                h.a("updateEventByKeyAndCategory", (Throwable) e2);
            }
        } else {
            b(str, str2, str3);
        }
    }

    public void b(String str, String str2, String str3) {
        i iVar;
        ContentValues contentValues = new ContentValues();
        contentValues.put("value", str3);
        synchronized (c) {
            try {
                c.getWritableDatabase().update("mistat_event", contentValues, "category=? AND key=?", new String[]{str2, str});
                iVar = c;
            } catch (SQLiteException e2) {
                try {
                    h.a("EventDAO", "Error to update data from DB, key=" + str, (Throwable) e2);
                    iVar = c;
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
            iVar.close();
        }
    }

    public List<StatEventPojo> a(long j) {
        if (!b) {
            return b(j);
        }
        g();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            do {
                if (this.f) {
                    if (this.e != null) {
                        List<StatEventPojo> a2 = this.e.a(j);
                        StringBuilder sb = new StringBuilder();
                        sb.append("process getAll, result size is :");
                        sb.append(a2 == null ? 0 : a2.size());
                        h.b(sb.toString());
                        return a2;
                    }
                }
            } while (System.currentTimeMillis() - currentTimeMillis <= 1000);
            return new ArrayList();
        } catch (Exception e2) {
            h.a("getAllEventOrderByTimestampDescend", (Throwable) e2);
            return new ArrayList();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x0080 A[SYNTHETIC, Splitter:B:25:0x0080] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.List<com.xiaomi.mistatistic.sdk.data.StatEventPojo> b(long r16) {
        /*
            r15 = this;
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            com.xiaomi.mistatistic.sdk.controller.i r2 = c
            monitor-enter(r2)
            com.xiaomi.mistatistic.sdk.controller.i r0 = c     // Catch:{ all -> 0x00b9 }
            android.database.sqlite.SQLiteDatabase r0 = r0.getReadableDatabase()     // Catch:{ all -> 0x00b9 }
            if (r0 != 0) goto L_0x0014
            monitor-exit(r2)     // Catch:{ all -> 0x00b9 }
            return r1
        L_0x0014:
            r12 = 0
            java.lang.String r4 = "mistat_event"
            r5 = 0
            java.lang.String r6 = "ts<?"
            r13 = 1
            java.lang.String[] r7 = new java.lang.String[r13]     // Catch:{ SQLiteException -> 0x009c }
            java.lang.String r3 = java.lang.String.valueOf(r16)     // Catch:{ SQLiteException -> 0x009c }
            r14 = 0
            r7[r14] = r3     // Catch:{ SQLiteException -> 0x009c }
            r8 = 0
            r9 = 0
            java.lang.String r10 = "ts DESC"
            r3 = 500(0x1f4, float:7.0E-43)
            java.lang.String r11 = java.lang.String.valueOf(r3)     // Catch:{ SQLiteException -> 0x009c }
            r3 = r0
            android.database.Cursor r11 = r3.query(r4, r5, r6, r7, r8, r9, r10, r11)     // Catch:{ SQLiteException -> 0x009c }
            if (r11 == 0) goto L_0x007d
            boolean r3 = r11.moveToLast()     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            if (r3 == 0) goto L_0x007d
            java.lang.String r3 = "ts"
            int r3 = r11.getColumnIndex(r3)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            long r3 = r11.getLong(r3)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            r11.close()     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            java.lang.String r6 = "ts<? AND ts>=? AND anonymous=?"
            r5 = 3
            java.lang.String[] r7 = new java.lang.String[r5]     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            java.lang.String r5 = java.lang.String.valueOf(r16)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            r7[r14] = r5     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            java.lang.String r3 = java.lang.String.valueOf(r3)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            r7[r13] = r3     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            r3 = 2
            r4 = r15
            boolean r4 = r4.d     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            if (r4 == 0) goto L_0x0064
            java.lang.String r4 = java.lang.String.valueOf(r13)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            goto L_0x0068
        L_0x0064:
            java.lang.String r4 = java.lang.String.valueOf(r14)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
        L_0x0068:
            r7[r3] = r4     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            java.lang.String r4 = "mistat_event"
            r5 = 0
            r8 = 0
            r9 = 0
            java.lang.String r10 = "ts DESC"
            r3 = r0
            android.database.Cursor r0 = r3.query(r4, r5, r6, r7, r8, r9, r10)     // Catch:{ SQLiteException -> 0x007a, all -> 0x0078 }
            r12 = r0
            goto L_0x007e
        L_0x0078:
            r0 = move-exception
            goto L_0x00b0
        L_0x007a:
            r0 = move-exception
            r12 = r11
            goto L_0x009d
        L_0x007d:
            r12 = r11
        L_0x007e:
            if (r12 == 0) goto L_0x0093
            boolean r0 = r12.moveToFirst()     // Catch:{ SQLiteException -> 0x009c }
            if (r0 == 0) goto L_0x0093
        L_0x0086:
            com.xiaomi.mistatistic.sdk.data.StatEventPojo r0 = a((android.database.Cursor) r12)     // Catch:{ SQLiteException -> 0x009c }
            r1.add(r0)     // Catch:{ SQLiteException -> 0x009c }
            boolean r0 = r12.moveToNext()     // Catch:{ SQLiteException -> 0x009c }
            if (r0 != 0) goto L_0x0086
        L_0x0093:
            r12.close()     // Catch:{ all -> 0x00b9 }
            com.xiaomi.mistatistic.sdk.controller.i r0 = c     // Catch:{ all -> 0x00b9 }
            goto L_0x00a9
        L_0x0099:
            r0 = move-exception
            r11 = r12
            goto L_0x00b0
        L_0x009c:
            r0 = move-exception
        L_0x009d:
            java.lang.String r3 = "EventDAO"
            java.lang.String r4 = "Error while reading data from DB"
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r3, (java.lang.String) r4, (java.lang.Throwable) r0)     // Catch:{ all -> 0x0099 }
            r12.close()     // Catch:{ all -> 0x00b9 }
            com.xiaomi.mistatistic.sdk.controller.i r0 = c     // Catch:{ all -> 0x00b9 }
        L_0x00a9:
            r0.close()     // Catch:{ all -> 0x00b9 }
            monitor-exit(r2)     // Catch:{ all -> 0x00b9 }
            return r1
        L_0x00b0:
            r11.close()     // Catch:{ all -> 0x00b9 }
            com.xiaomi.mistatistic.sdk.controller.i r1 = c     // Catch:{ all -> 0x00b9 }
            r1.close()     // Catch:{ all -> 0x00b9 }
            throw r0     // Catch:{ all -> 0x00b9 }
        L_0x00b9:
            r0 = move-exception
            monitor-exit(r2)     // Catch:{ all -> 0x00b9 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.f.b(long):java.util.List");
    }

    public void b() {
        if (b) {
            try {
                Intent intent = new Intent(c.a(), Class.forName(a));
                intent.putExtra("type", 3);
                c.a().startService(intent);
            } catch (Exception e2) {
                h.a("deleteOldEvents", (Throwable) e2);
            }
        } else {
            c();
        }
    }

    public void c() {
        i iVar;
        long currentTimeMillis = System.currentTimeMillis() - 259200000;
        synchronized (c) {
            try {
                int delete = c.getWritableDatabase().delete("mistat_event", "ts<=? and category <> ?", new String[]{String.valueOf(currentTimeMillis), "mistat_basic"});
                if (delete > 0) {
                    MiStatInterface.recordCalculateEvent("quality_monitor", "delete_old_events", (long) delete);
                }
                iVar = c;
            } catch (SQLiteException e2) {
                try {
                    h.a("EventDAO", "Error while deleting out-of-date data from DB", (Throwable) e2);
                    iVar = c;
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
            iVar.close();
        }
    }

    public int d() {
        if (!b) {
            return e();
        }
        g();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            do {
                if (this.f) {
                    if (this.e != null) {
                        int a2 = this.e.a();
                        h.b("process getCount , result is:" + a2);
                        return a2;
                    }
                }
            } while (System.currentTimeMillis() - currentTimeMillis <= 1000);
            return 0;
        } catch (Exception e2) {
            h.a("getEventCount", (Throwable) e2);
            return 0;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x0053 A[SYNTHETIC, Splitter:B:29:0x0053] */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0060 A[Catch:{ SQLiteException -> 0x003a, all -> 0x0037, all -> 0x0064 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int e() {
        /*
            r13 = this;
            com.xiaomi.mistatistic.sdk.controller.i r0 = c
            monitor-enter(r0)
            r1 = 0
            r2 = 0
            com.xiaomi.mistatistic.sdk.controller.i r3 = c     // Catch:{ SQLiteException -> 0x0049 }
            android.database.sqlite.SQLiteDatabase r4 = r3.getReadableDatabase()     // Catch:{ SQLiteException -> 0x0049 }
            java.lang.String r5 = "mistat_event"
            java.lang.String r3 = "count(*)"
            java.lang.String[] r6 = new java.lang.String[]{r3}     // Catch:{ SQLiteException -> 0x0049 }
            r7 = 0
            r8 = 0
            r9 = 0
            r10 = 0
            r11 = 0
            android.database.Cursor r3 = r4.query(r5, r6, r7, r8, r9, r10, r11)     // Catch:{ SQLiteException -> 0x0049 }
            if (r3 == 0) goto L_0x003f
            boolean r2 = r3.moveToFirst()     // Catch:{ SQLiteException -> 0x003a, all -> 0x0037 }
            if (r2 == 0) goto L_0x003f
            int r2 = r3.getInt(r1)     // Catch:{ SQLiteException -> 0x003a, all -> 0x0037 }
            if (r3 == 0) goto L_0x0030
            r3.close()     // Catch:{ all -> 0x0064 }
        L_0x0030:
            com.xiaomi.mistatistic.sdk.controller.i r1 = c     // Catch:{ all -> 0x0064 }
            r1.close()     // Catch:{ all -> 0x0064 }
            monitor-exit(r0)     // Catch:{ all -> 0x0064 }
            return r2
        L_0x0037:
            r1 = move-exception
            r2 = r3
            goto L_0x005e
        L_0x003a:
            r2 = move-exception
            r12 = r3
            r3 = r2
            r2 = r12
            goto L_0x004a
        L_0x003f:
            if (r3 == 0) goto L_0x0044
            r3.close()     // Catch:{ all -> 0x0064 }
        L_0x0044:
            com.xiaomi.mistatistic.sdk.controller.i r2 = c     // Catch:{ all -> 0x0064 }
            goto L_0x0058
        L_0x0047:
            r1 = move-exception
            goto L_0x005e
        L_0x0049:
            r3 = move-exception
        L_0x004a:
            java.lang.String r4 = "EventDAO"
            java.lang.String r5 = "Error while getting count from DB"
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r4, (java.lang.String) r5, (java.lang.Throwable) r3)     // Catch:{ all -> 0x0047 }
            if (r2 == 0) goto L_0x0056
            r2.close()     // Catch:{ all -> 0x0064 }
        L_0x0056:
            com.xiaomi.mistatistic.sdk.controller.i r2 = c     // Catch:{ all -> 0x0064 }
        L_0x0058:
            r2.close()     // Catch:{ all -> 0x0064 }
            monitor-exit(r0)     // Catch:{ all -> 0x0064 }
            return r1
        L_0x005e:
            if (r2 == 0) goto L_0x0066
            r2.close()     // Catch:{ all -> 0x0064 }
            goto L_0x0066
        L_0x0064:
            r1 = move-exception
            goto L_0x006c
        L_0x0066:
            com.xiaomi.mistatistic.sdk.controller.i r2 = c     // Catch:{ all -> 0x0064 }
            r2.close()     // Catch:{ all -> 0x0064 }
            throw r1     // Catch:{ all -> 0x0064 }
        L_0x006c:
            monitor-exit(r0)     // Catch:{ all -> 0x0064 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.f.e():int");
    }

    public static StatEventPojo a(Cursor cursor) {
        StatEventPojo statEventPojo = new StatEventPojo();
        long j = cursor.getLong(2);
        String string = cursor.getString(4);
        String string2 = cursor.getString(5);
        String string3 = cursor.getString(1);
        String string4 = cursor.getString(3);
        String string5 = cursor.getString(6);
        int i = cursor.getInt(7);
        statEventPojo.category = string3;
        statEventPojo.key = string4;
        statEventPojo.value = string;
        statEventPojo.timeStamp = j;
        statEventPojo.type = string2;
        statEventPojo.extra = string5;
        statEventPojo.anonymous = i;
        return statEventPojo;
    }

    public void a(long j, long j2) {
        if (b) {
            try {
                Intent intent = new Intent(c.a(), Class.forName(a));
                intent.putExtra("type", 5);
                intent.putExtra("startTime", j);
                intent.putExtra("endTime", j2);
                c.a().startService(intent);
            } catch (Exception e2) {
                h.a("deleteEventsByStartAndEndTS", (Throwable) e2);
            }
        } else {
            b(j, j2);
        }
    }

    public void b(long j, long j2) {
        i iVar;
        synchronized (c) {
            try {
                h.b("EventDAO", "deleteEventsByStartAndEndTS, start:%d, end:%d", Long.valueOf(j), Long.valueOf(j2));
                SQLiteDatabase writableDatabase = c.getWritableDatabase();
                String[] strArr = new String[3];
                strArr[0] = String.valueOf(j2);
                strArr[1] = String.valueOf(j);
                strArr[2] = this.d ? String.valueOf(1) : String.valueOf(0);
                writableDatabase.delete("mistat_event", "ts<=? AND ts>=? AND anonymous=?", strArr);
                iVar = c;
            } catch (SQLiteException e2) {
                try {
                    h.a("EventDAO", "Error while deleting event by ts from DB", (Throwable) e2);
                    iVar = c;
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
            iVar.close();
        }
    }

    public boolean f() {
        boolean z;
        i iVar;
        synchronized (c) {
            z = false;
            try {
                Cursor query = c.getReadableDatabase().query("mistat_event", null, "anonymous=?", new String[]{String.valueOf(1)}, null, null, "ts DESC", String.valueOf(500));
                if (query != null && query.moveToLast()) {
                    z = true;
                }
                iVar = c;
            } catch (SQLiteException e2) {
                try {
                    h.a("EventDAO", "Error while isExistAnonymousData from DB", (Throwable) e2);
                    iVar = c;
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
            iVar.close();
        }
        return z;
    }
}
