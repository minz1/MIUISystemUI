package com.xiaomi.mistatistic.sdk.data;

import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import com.xiaomi.mistatistic.sdk.controller.h;
import com.xiaomi.mistatistic.sdk.controller.q;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/* compiled from: CustomNumericEvent */
public abstract class f extends AbstractEvent {
    protected long a;
    private String b;
    private String c;
    private Map<String, String> d;

    public abstract String a();

    public f(String str, String str2, long j, Map<String, String> map) {
        this.b = str;
        this.c = str2;
        this.a = j;
        if (this.b.equals("mistat_basic") && (BuildSetting.isInternationalBuild() || q.c())) {
            setAnonymous(1);
        }
        if (map == null) {
            this.d = null;
        } else {
            this.d = new HashMap(map);
        }
    }

    public String getCategory() {
        return this.b;
    }

    public JSONObject valueToJSon() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("category", this.b);
        jSONObject.put("key", this.c);
        jSONObject.put("type", a());
        jSONObject.put("value", this.a);
        if (this.d != null) {
            jSONObject.put("params", new JSONObject(this.d));
        }
        return jSONObject;
    }

    public StatEventPojo toPojo() {
        StatEventPojo statEventPojo = new StatEventPojo();
        statEventPojo.category = this.b;
        statEventPojo.key = this.c;
        statEventPojo.timeStamp = this.mTS;
        statEventPojo.type = a();
        statEventPojo.value = String.valueOf(this.a);
        statEventPojo.extra = a(this.d);
        statEventPojo.anonymous = getAnonymous();
        return statEventPojo;
    }

    private String a(Map<String, String> map) {
        if (map != null) {
            try {
                if (!map.isEmpty()) {
                    JSONObject jSONObject = new JSONObject();
                    for (String next : map.keySet()) {
                        jSONObject.put(next, map.get(next));
                    }
                    return jSONObject.toString();
                }
            } catch (JSONException e) {
                h.a("json error", (Throwable) e);
            }
        }
        return null;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof f)) {
            return false;
        }
        f fVar = (f) obj;
        if (TextUtils.equals(this.b, fVar.b) && TextUtils.equals(this.c, fVar.c) && TextUtils.equals(a(), fVar.a()) && this.a == fVar.a && this.d != null) {
            z = this.d.equals(fVar.d);
        }
        return z;
    }
}
