package com.android.systemui.miui.statusbar.analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class FoldClickEvent extends FoldEvent {
    protected final String CLICK_TIMESTAMP = "click_timestamp";
    private long mClickTimestamp = System.currentTimeMillis();

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "click", jsonObject.toString());
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        super.wrapJSONObject(jsonObject);
        if (this.mClickTimestamp > 0) {
            try {
                jsonObject.put("click_timestamp", this.mClickTimestamp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            jsonObject.put("event", "click");
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return jsonObject;
    }
}
