package com.xiaomi.mistatistic.sdk.data;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractEvent {
    private int anonymous = 0;
    protected long mTS = System.currentTimeMillis();

    public abstract String getCategory();

    public abstract StatEventPojo toPojo();

    public abstract JSONObject valueToJSon() throws JSONException;

    public int getAnonymous() {
        return this.anonymous;
    }

    public void setAnonymous(int i) {
        this.anonymous = i;
    }
}
