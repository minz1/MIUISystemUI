package com.xiaomi.mistatistic.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.xiaomi.mistatistic.sdk.controller.LocalEventRecorder;
import com.xiaomi.mistatistic.sdk.controller.asyncjobs.a;
import com.xiaomi.mistatistic.sdk.controller.c;
import com.xiaomi.mistatistic.sdk.controller.e;
import com.xiaomi.mistatistic.sdk.controller.f;
import com.xiaomi.mistatistic.sdk.controller.h;
import com.xiaomi.mistatistic.sdk.controller.j;
import com.xiaomi.mistatistic.sdk.controller.p;
import com.xiaomi.mistatistic.sdk.controller.q;
import java.util.Map;

public abstract class MiStatInterface {
    private static boolean sABTestInitialized = false;
    private static boolean sInitialized = false;

    public static final void initialize(Context context, String str, String str2, String str3) {
        initialize(context, str, str2, str3, false);
    }

    public static final void initialize(Context context, String str, String str2, String str3, boolean z) {
        if (context != null) {
            Log.d("MI_STAT", String.format("initialize %s, %s, %s, %s", new Object[]{context.getPackageName(), str, str3, Boolean.valueOf(z)}));
            if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
                throw new IllegalArgumentException("appID or appKey is empty.");
            }
            Context applicationContext = context.getApplicationContext();
            if (applicationContext == null) {
                applicationContext = context;
            }
            j.d(context);
            if (TextUtils.isEmpty(str3)) {
                str3 = "mistats_default";
            }
            c.a(applicationContext, str, str2, str3);
            f.a();
            new e().a();
            p.a().b();
            sInitialized = true;
            if (z) {
                URLStatsRecorder.enableAutoRecord();
                return;
            }
            return;
        }
        throw new IllegalArgumentException("Initializing sdk error, reason: context is null.");
    }

    public static final void setUploadPolicy(int i, long j) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        if (i != 4 || (j >= 60000 && j <= 86400000)) {
            p.a().a(i, j);
            return;
        }
        throw new IllegalArgumentException("interval should be set between 1 minutes and 1 day");
    }

    public static final void enableLog() {
        h.a();
    }

    public static final void recordCountEvent(String str, String str2) {
        recordCountEvent(str, str2, null);
    }

    public static final void recordCountEvent(String str, String str2, Map<String, String> map) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        checkCategoryAndKey(str, str2);
        if (TextUtils.isEmpty(str)) {
            str = "default_category";
        }
        LocalEventRecorder.insertEvent(new com.xiaomi.mistatistic.sdk.data.e(str, str2, map));
        a.c();
    }

    public static final void recordCalculateEvent(String str, String str2, long j) {
        recordCalculateEvent(str, str2, j, null);
    }

    public static final void recordCalculateEvent(String str, String str2, long j, Map<String, String> map) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        checkCategoryAndKey(str, str2);
        if (TextUtils.isEmpty(str)) {
            str = "default_category";
        }
        com.xiaomi.mistatistic.sdk.data.c cVar = new com.xiaomi.mistatistic.sdk.data.c(str, str2, j, map);
        LocalEventRecorder.insertEvent(cVar);
        a.c();
    }

    public static final void recordStringPropertyEvent(String str, String str2, String str3) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        checkCategoryAndKey(str, str2);
        if (TextUtils.isEmpty(str)) {
            str = "default_category";
        }
        LocalEventRecorder.insertEvent(new com.xiaomi.mistatistic.sdk.data.h(str, str2, str3));
        a.c();
    }

    public static final void recordCountEventAnonymous(String str, String str2) {
        recordCountEventAnonymous(str, str2, null);
    }

    public static final void recordCountEventAnonymous(String str, String str2, Map<String, String> map) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        checkCategoryAndKey(str, str2);
        if (TextUtils.isEmpty(str)) {
            str = "default_category";
        }
        com.xiaomi.mistatistic.sdk.data.e eVar = new com.xiaomi.mistatistic.sdk.data.e(str, str2, map);
        eVar.setAnonymous(1);
        LocalEventRecorder.insertEvent(eVar);
        a.c();
    }

    public static final void recordCalculateEventAnonymous(String str, String str2, long j) {
        recordCalculateEventAnonymous(str, str2, j, null);
    }

    public static final void recordCalculateEventAnonymous(String str, String str2, long j, Map<String, String> map) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        checkCategoryAndKey(str, str2);
        if (TextUtils.isEmpty(str)) {
            str = "default_category";
        }
        com.xiaomi.mistatistic.sdk.data.c cVar = new com.xiaomi.mistatistic.sdk.data.c(str, str2, j, map);
        cVar.setAnonymous(1);
        LocalEventRecorder.insertEvent(cVar);
        a.c();
    }

    public static final void recordStringPropertyEventAnonymous(String str, String str2, String str3) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        checkInitialized();
        checkCategoryAndKey(str, str2);
        if (TextUtils.isEmpty(str)) {
            str = "default_category";
        }
        com.xiaomi.mistatistic.sdk.data.h hVar = new com.xiaomi.mistatistic.sdk.data.h(str, str2, str3);
        hVar.setAnonymous(1);
        LocalEventRecorder.insertEvent(hVar);
        a.c();
    }

    private static void checkInitialized() {
        if (!sInitialized) {
            throw new IllegalStateException("not initialized, do you forget to call initialize when application started?");
        }
    }

    private static void checkCategoryAndKey(String str, String str2) {
        if (!TextUtils.isEmpty(str) && str.startsWith("mistat_")) {
            throw new IllegalArgumentException("category cannot start with mistat_");
        } else if (!TextUtils.isEmpty(str2) && str2.startsWith("mistat_")) {
            throw new IllegalArgumentException("key cannot start with mistat_");
        }
    }

    public static void enableExceptionCatcher(boolean z) {
        if (!q.b()) {
            Log.w("MI_STAT", "The statistics is disabled.");
            return;
        }
        if (!isExceptionCatcherEnabled()) {
            b.a(false);
        }
        b.a(z ? 2 : 3);
    }

    public static boolean isExceptionCatcherEnabled() {
        return b.d() != 1;
    }

    public static boolean shouldExceptionUploadImmediately() {
        return b.d() == 2;
    }
}
