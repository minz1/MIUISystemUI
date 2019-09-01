package com.xiaomi.analytics;

import android.text.TextUtils;
import android.util.Log;
import com.xiaomi.analytics.internal.util.ALog;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

public abstract class Action {
    private static Set<String> sKeywords = new HashSet();
    private JSONObject mContent = new JSONObject();
    private JSONObject mExtra = new JSONObject();

    static {
        sKeywords.add("_event_id_");
        sKeywords.add("_category_");
        sKeywords.add("_action_");
        sKeywords.add("_label_");
        sKeywords.add("_value_");
    }

    public Action addParam(String key, long value) {
        ensureKey(key);
        addContent(key, value);
        return this;
    }

    public Action addParam(String key, String value) {
        ensureKey(key);
        addContent(key, (Object) value);
        return this;
    }

    /* access modifiers changed from: package-private */
    public void addContent(String key, long value) {
        if (!TextUtils.isEmpty(key)) {
            try {
                this.mContent.put(key, value);
            } catch (Exception e) {
                Log.e(ALog.addPrefix("Action"), "addContent long value e", e);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void addContent(String key, Object value) {
        if (!TextUtils.isEmpty(key)) {
            try {
                this.mContent.put(key, value);
            } catch (Exception e) {
                Log.e(ALog.addPrefix("Action"), "addContent Object value e", e);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void addExtra(String key, String value) {
        try {
            this.mExtra.put(key, value);
        } catch (Exception e) {
            Log.e(ALog.addPrefix("Action"), "addExtra e", e);
        }
    }

    private void ensureKey(String key) {
        if (!TextUtils.isEmpty(key) && sKeywords.contains(key)) {
            throw new IllegalArgumentException("this key " + key + " is built-in, please pick another key.");
        }
    }

    /* access modifiers changed from: package-private */
    public final JSONObject getContent() {
        return this.mContent;
    }

    /* access modifiers changed from: package-private */
    public final JSONObject getExtra() {
        return this.mExtra;
    }
}
