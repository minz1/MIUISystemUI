package com.android.systemui.miui.statusbar.analytics;

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class UploadLocalAlgoModelEvent implements INotificationEvent {
    private String mLocalAlgoModel;

    public UploadLocalAlgoModelEvent(String localAlgoModel) {
        this.mLocalAlgoModel = localAlgoModel;
    }

    public JSONObject wrapJSONObject(JSONObject jsonObject) {
        if (!TextUtils.isEmpty(this.mLocalAlgoModel)) {
            try {
                jsonObject.put("params", this.mLocalAlgoModel);
            } catch (JSONException e) {
            }
        }
        try {
            jsonObject.put("event", "local_algo_model");
        } catch (JSONException e2) {
        }
        return jsonObject;
    }

    public TinyData getTinyData() {
        return new TinyData("notification", "local_algo_model", wrapJSONObject(new JSONObject()).toString());
    }
}
