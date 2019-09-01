package com.xiaomi.mistatistic.sdk.controller.asyncjobs;

import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.controller.d;
import com.xiaomi.mistatistic.sdk.controller.f;
import com.xiaomi.mistatistic.sdk.controller.h;
import com.xiaomi.mistatistic.sdk.controller.l;
import com.xiaomi.mistatistic.sdk.data.StatEventPojo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* compiled from: RemoteDataPackingJob */
public class b implements d.a {
    private static int a = 0;
    private static int b = 0;
    private long c;
    private a d;
    private HashMap<String, JSONObject> e = new HashMap<>();
    private HashMap<String, JSONObject> f = new HashMap<>();
    private ArrayList<String> g = new ArrayList<>();
    private JSONObject h = null;
    private long i = 0;
    private boolean j = false;

    /* compiled from: RemoteDataPackingJob */
    public interface a {
        void a(String str, long j, long j2, int i, boolean z);
    }

    /* renamed from: com.xiaomi.mistatistic.sdk.controller.asyncjobs.b$b  reason: collision with other inner class name */
    /* compiled from: RemoteDataPackingJob */
    public class C0002b {
        int a;
        /* access modifiers changed from: private */
        public JSONArray c;
        /* access modifiers changed from: private */
        public long d;
        /* access modifiers changed from: private */
        public long e;

        public C0002b(JSONArray jSONArray, long j, long j2, int i) {
            this.c = jSONArray;
            this.d = j;
            this.e = j2;
            this.a = i;
        }
    }

    public b(long j2, boolean z, a aVar) {
        this.c = j2;
        this.d = aVar;
        this.j = z;
    }

    public void a() {
        try {
            C0002b a2 = a(Long.MAX_VALUE);
            if (a2.c != null) {
                this.d.a(a2.c.toString(), a2.d, a2.e, a2.a, this.j);
            } else {
                this.d.a("", a2.d, a2.e, a2.a, this.j);
            }
            if (this.j) {
                if (a2.a >= 500) {
                    h.a(String.format("Packing %d anonymous events over MAX_PACKING_EVENT %d", new Object[]{Integer.valueOf(a2.a), 500}));
                    if (b < 5) {
                        new l().a();
                        b++;
                        return;
                    }
                    h.d("Packing, exceeded MAX_UPLOAD_TIMES 5");
                    return;
                }
                b = 0;
            } else if (a2.a >= 500) {
                h.a(String.format("Packing %d not anonymous events over MAX_PACKING_EVENT %d", new Object[]{Integer.valueOf(a2.a), 500}));
                if (a < 5) {
                    new l().a();
                    a++;
                    return;
                }
                h.d("Packing, exceeded MAX_UPLOAD_TIMES 5");
            } else {
                a = 0;
            }
        } catch (Exception e2) {
            h.a("remote data packing job execute exception:", (Throwable) e2);
            this.d.a("", 0, 0, 0, this.j);
        }
    }

    private void a(StatEventPojo statEventPojo) {
        long j2 = statEventPojo.timeStamp;
        if (c(j2) && this.h != null) {
            b();
            this.i = j2;
        }
    }

    private boolean c(long j2) {
        return (this.c > 0 && this.i - j2 > this.c) || !b(j2);
    }

    public C0002b a(long j2) throws JSONException {
        int i2;
        long j3;
        long j4;
        JSONArray jSONArray;
        long j5;
        int i3;
        long j6;
        JSONArray jSONArray2 = new JSONArray();
        f fVar = new f();
        fVar.b();
        b();
        fVar.a(Boolean.valueOf(this.j));
        List<StatEventPojo> a2 = fVar.a(j2);
        int i4 = 0;
        long j7 = 0;
        if (a2 != null) {
            try {
                if (a2.size() > 0) {
                    i3 = 0;
                    j3 = 0;
                    j6 = 0;
                    while (i4 < a2.size()) {
                        try {
                            StatEventPojo statEventPojo = a2.get(i4);
                            if (j3 == 0) {
                                long j8 = statEventPojo.timeStamp;
                                try {
                                    this.i = j8;
                                    j3 = j8;
                                } catch (SQLiteException e2) {
                                    e = e2;
                                    j3 = j8;
                                    j5 = j6;
                                    h.a("packing exception:", (Throwable) e);
                                    jSONArray = jSONArray2;
                                    i2 = i3;
                                    j4 = j5;
                                    C0002b bVar = new C0002b(jSONArray, j4, j3, i2);
                                    return bVar;
                                }
                            }
                            j5 = statEventPojo.timeStamp;
                            try {
                                a(statEventPojo);
                                if (this.h == null) {
                                    this.h = new JSONObject();
                                    this.h.put("endTS", statEventPojo.timeStamp);
                                    this.h.put("content", new JSONArray());
                                    jSONArray2.put(this.h);
                                }
                                if ("mistat_session".equals(statEventPojo.category)) {
                                    b(statEventPojo);
                                } else if ("mistat_pv".equals(statEventPojo.category)) {
                                    d(statEventPojo);
                                } else if ("mistat_pt".equals(statEventPojo.category)) {
                                    e(statEventPojo);
                                } else if ("mistat_session_extra".equals(statEventPojo.category)) {
                                    c(statEventPojo);
                                } else {
                                    f(statEventPojo);
                                }
                                this.h.put("startTS", statEventPojo.timeStamp);
                                i3++;
                                i4++;
                                j6 = j5;
                            } catch (SQLiteException e3) {
                                e = e3;
                                h.a("packing exception:", (Throwable) e);
                                jSONArray = jSONArray2;
                                i2 = i3;
                                j4 = j5;
                                C0002b bVar2 = new C0002b(jSONArray, j4, j3, i2);
                                return bVar2;
                            }
                        } catch (SQLiteException e4) {
                            e = e4;
                            j5 = j6;
                            h.a("packing exception:", (Throwable) e);
                            jSONArray = jSONArray2;
                            i2 = i3;
                            j4 = j5;
                            C0002b bVar22 = new C0002b(jSONArray, j4, j3, i2);
                            return bVar22;
                        }
                    }
                    h.a("Packing complete, total " + a2.size() + " records were packed and to be uploaded");
                    j7 = j3;
                    i4 = i3;
                    j3 = j7;
                    j4 = j6;
                    i2 = i4;
                    jSONArray = jSONArray2;
                    C0002b bVar222 = new C0002b(jSONArray, j4, j3, i2);
                    return bVar222;
                }
            } catch (SQLiteException e5) {
                e = e5;
                i3 = 0;
                j3 = 0;
                j5 = 0;
                h.a("packing exception:", (Throwable) e);
                jSONArray = jSONArray2;
                i2 = i3;
                j4 = j5;
                C0002b bVar2222 = new C0002b(jSONArray, j4, j3, i2);
                return bVar2222;
            }
        }
        h.a("No data available to be packed");
        jSONArray2 = null;
        j6 = 0;
        j3 = j7;
        j4 = j6;
        i2 = i4;
        jSONArray = jSONArray2;
        C0002b bVar22222 = new C0002b(jSONArray, j4, j3, i2);
        return bVar22222;
    }

    /* access modifiers changed from: package-private */
    public boolean b(long j2) {
        long j3 = this.i - (this.i % 86400000);
        return j2 >= j3 && j2 < 86400000 + j3;
    }

    private void b() {
        this.h = null;
        this.e.clear();
        this.g.clear();
        this.f.clear();
    }

    private void b(StatEventPojo statEventPojo) throws JSONException {
        JSONObject jSONObject = this.e.get("mistat_session");
        if (jSONObject == null) {
            JSONArray jSONArray = new JSONArray();
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("category", "mistat_session");
            jSONObject2.put("values", jSONArray);
            this.e.put("mistat_session", jSONObject2);
            this.h.getJSONArray("content").put(jSONObject2);
            jSONObject = jSONObject2;
        }
        JSONObject jSONObject3 = new JSONObject();
        String[] split = statEventPojo.value.split(",");
        long parseLong = Long.parseLong(split[0]);
        long parseLong2 = Long.parseLong(split[1]);
        jSONObject3.put("start", parseLong);
        jSONObject3.put("end", parseLong2);
        jSONObject3.put("env", statEventPojo.extra);
        jSONObject.getJSONArray("values").put(jSONObject3);
    }

    private void c(StatEventPojo statEventPojo) throws JSONException {
        JSONObject jSONObject = this.e.get("mistat_session_extra");
        if (jSONObject == null) {
            JSONArray jSONArray = new JSONArray();
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("category", "mistat_session_extra");
            jSONObject2.put("values", jSONArray);
            this.e.put("mistat_session_extra", jSONObject2);
            this.h.getJSONArray("content").put(jSONObject2);
            jSONObject = jSONObject2;
        }
        JSONObject jSONObject3 = new JSONObject();
        long parseLong = Long.parseLong(statEventPojo.value);
        long parseLong2 = Long.parseLong(statEventPojo.extra);
        jSONObject3.put("start", parseLong);
        jSONObject3.put("auto_end", parseLong2);
        jSONObject.getJSONArray("values").put(jSONObject3);
    }

    private void d(StatEventPojo statEventPojo) throws JSONException {
        JSONObject jSONObject = this.e.get("mistat_pv");
        if (jSONObject == null) {
            jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray();
            JSONArray jSONArray2 = new JSONArray();
            jSONObject.put("category", "mistat_pv");
            jSONObject.put("values", jSONArray);
            jSONObject.put("source", jSONArray2);
            this.e.put("mistat_pv", jSONObject);
            this.h.getJSONArray("content").put(jSONObject);
        }
        String[] split = statEventPojo.value.trim().split(",");
        String[] strArr = new String[split.length];
        if (split != null && split.length > 0) {
            for (int i2 = 0; i2 < split.length; i2++) {
                int indexOf = this.g.indexOf(split[i2]);
                if (indexOf >= 0) {
                    strArr[i2] = String.valueOf(indexOf + 1);
                } else {
                    strArr[i2] = String.valueOf(this.g.size() + 1);
                    this.g.add(split[i2]);
                }
            }
        }
        jSONObject.getJSONArray("values").put(TextUtils.join(",", strArr));
        jSONObject.put("index", TextUtils.join(",", this.g));
        if (TextUtils.isEmpty(statEventPojo.extra)) {
            jSONObject.getJSONArray("source").put("");
        } else {
            jSONObject.getJSONArray("source").put(statEventPojo.extra);
        }
    }

    private void e(StatEventPojo statEventPojo) throws JSONException {
        JSONObject jSONObject = this.e.get("mistat_pt");
        if (jSONObject == null) {
            jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray();
            jSONObject.put("category", "mistat_pt");
            jSONObject.put("values", jSONArray);
            this.e.put("mistat_pt", jSONObject);
            this.h.getJSONArray("content").put(jSONObject);
        }
        JSONArray jSONArray2 = jSONObject.getJSONArray("values");
        for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
            JSONObject jSONObject2 = jSONArray2.getJSONObject(i2);
            if (TextUtils.equals(jSONObject2.getString("key"), statEventPojo.key)) {
                jSONObject2.put("value", jSONObject2.getString("value") + "," + statEventPojo.value);
                return;
            }
        }
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("key", statEventPojo.key);
        jSONObject3.put("value", statEventPojo.value);
        jSONObject.getJSONArray("values").put(jSONObject3);
    }

    private void f(StatEventPojo statEventPojo) throws JSONException {
        boolean z;
        JSONObject jSONObject = this.e.get(statEventPojo.category);
        if (jSONObject == null) {
            jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray();
            jSONObject.put("category", statEventPojo.category);
            jSONObject.put("values", jSONArray);
            this.e.put(statEventPojo.category, jSONObject);
            this.h.getJSONArray("content").put(jSONObject);
            z = true;
        } else {
            z = false;
        }
        if ("event".equals(statEventPojo.type) && TextUtils.isEmpty(statEventPojo.extra)) {
            JSONObject jSONObject2 = this.f.get(statEventPojo.key);
            if (jSONObject2 == null || z) {
                JSONObject jSONObject3 = new JSONObject();
                jSONObject3.put("key", statEventPojo.key);
                jSONObject3.put("type", statEventPojo.type);
                jSONObject3.put("value", Long.parseLong(statEventPojo.value));
                jSONObject.getJSONArray("values").put(jSONObject3);
                this.f.put(statEventPojo.key, jSONObject3);
                return;
            }
            jSONObject2.put("value", jSONObject2.getLong("value") + Long.parseLong(statEventPojo.value));
        } else if ("mistat_extra".equals(statEventPojo.category)) {
            jSONObject.getJSONArray("values").put(statEventPojo.value);
        } else {
            JSONObject jSONObject4 = new JSONObject();
            jSONObject4.put("key", statEventPojo.key);
            jSONObject4.put("type", statEventPojo.type);
            if ("count".equals(statEventPojo.type) || "numeric".equals(statEventPojo.type)) {
                jSONObject4.put("value", Long.parseLong(statEventPojo.value));
            } else {
                jSONObject4.put("value", statEventPojo.value);
            }
            if (!TextUtils.isEmpty(statEventPojo.extra)) {
                jSONObject4.put("params", new JSONObject(statEventPojo.extra));
            }
            jSONObject.getJSONArray("values").put(jSONObject4);
        }
    }
}
