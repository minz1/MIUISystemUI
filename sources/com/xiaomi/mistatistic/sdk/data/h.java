package com.xiaomi.mistatistic.sdk.data;

import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import com.xiaomi.mistatistic.sdk.controller.q;
import org.json.JSONException;
import org.json.JSONObject;

/* compiled from: CustomStringPropertyEvent */
public class h extends AbstractEvent {
    private String a;
    private String b;
    private String c;

    public h(String str, String str2, String str3) {
        this.a = str;
        this.b = str2;
        this.c = str3;
        if (!this.a.equals("mistat_basic")) {
            return;
        }
        if (BuildSetting.isInternationalBuild() || q.c()) {
            setAnonymous(1);
        }
    }

    public String getCategory() {
        return this.a;
    }

    public JSONObject valueToJSon() throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("category", this.a);
        jSONObject.put("key", this.b);
        jSONObject.put("type", "property");
        jSONObject.put("value", this.c);
        return jSONObject;
    }

    public StatEventPojo toPojo() {
        StatEventPojo statEventPojo = new StatEventPojo();
        statEventPojo.category = this.a;
        statEventPojo.key = this.b;
        statEventPojo.timeStamp = this.mTS;
        statEventPojo.value = this.c;
        statEventPojo.type = "property";
        statEventPojo.anonymous = getAnonymous();
        return statEventPojo;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof h)) {
            return false;
        }
        h hVar = (h) obj;
        if (!TextUtils.equals(this.a, hVar.a) || !TextUtils.equals(this.b, hVar.b) || !TextUtils.equals(this.c, hVar.c)) {
            z = false;
        }
        return z;
    }
}
