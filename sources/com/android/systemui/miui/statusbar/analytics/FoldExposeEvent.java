package com.android.systemui.miui.statusbar.analytics;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FoldExposeEvent extends FoldEvent {
    private List<ExposeMessage> messageList;

    public FoldExposeEvent(List<ExposeMessage> exposeMessages) {
        this.messageList = new ArrayList(exposeMessages);
    }

    public TinyData getTinyData() {
        JSONObject jsonObject = new JSONObject();
        wrapJSONObject(jsonObject);
        return new TinyData("notification", "expose", jsonObject.toString());
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        super.wrapJSONObject(jsonObject);
        if (this.messageList != null) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < this.messageList.size(); i++) {
                jsonArray.put(this.messageList.get(i).toJSONObject().toString());
            }
            try {
                jsonObject.put("messageList", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            jsonObject.put("event", "expose");
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return jsonObject;
    }
}
