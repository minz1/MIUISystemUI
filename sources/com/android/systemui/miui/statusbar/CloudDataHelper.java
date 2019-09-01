package com.android.systemui.miui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import miui.util.NotificationFilterHelper;
import org.json.JSONArray;
import org.json.JSONException;

public class CloudDataHelper {
    public static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");

    public static void updateAll(Context context) {
        Log.d("CloudDataHelper", "CloudData updateAll");
        ContentResolver resolver = context.getContentResolver();
        String floatData = getCloudDataString(resolver, "systemui_float_whitelist", "whitelist");
        if (!TextUtils.isEmpty(floatData)) {
            int thisHashCode = floatData.hashCode();
            int lastHashCode = readHashCode(context, "systemui_float_whitelist");
            Log.d("CloudDataHelper", String.format("updateFloatWhitelist thisHashCode=%d lastHashCode=%d", new Object[]{Integer.valueOf(thisHashCode), Integer.valueOf(lastHashCode)}));
            if (thisHashCode != lastHashCode) {
                updateFloatWhitelist(context, floatData);
                writeHashCode(context, "systemui_float_whitelist", thisHashCode);
            }
        }
        String keyguardData = getCloudDataString(resolver, "systemui_keyguard_whitelist", "whitelist");
        if (!TextUtils.isEmpty(keyguardData)) {
            int thisHashCode2 = keyguardData.hashCode();
            int lastHashCode2 = readHashCode(context, "systemui_keyguard_whitelist");
            Log.d("CloudDataHelper", String.format("updateKeyguardWhitelist thisHashCode=%d lastHashCode=%d", new Object[]{Integer.valueOf(thisHashCode2), Integer.valueOf(lastHashCode2)}));
            if (thisHashCode2 != lastHashCode2) {
                updateKeyguardWhitelist(context, keyguardData);
                writeHashCode(context, "systemui_keyguard_whitelist", thisHashCode2);
            }
        }
        List<MiuiSettings.SettingsCloudData.CloudData> rules = getCloudDataList(resolver, "systemui_local_score");
        if (rules != null && !rules.isEmpty()) {
            int thisHashCode3 = rules.hashCode();
            int lastHashCode3 = readHashCode(context, "systemui_local_score");
            Log.d("CloudDataHelper", String.format("updateLocalAlgoModel thisHashCode=%d lastHashCode=%d", new Object[]{Integer.valueOf(thisHashCode3), Integer.valueOf(lastHashCode3)}));
            if (!LocalAlgoModel.hasLocalRules() || thisHashCode3 != lastHashCode3) {
                updateLocalAlgoModel(rules);
                writeHashCode(context, "systemui_local_score", thisHashCode3);
            }
        }
    }

    private static String getCloudDataString(ContentResolver resolver, String module, String key) {
        if (TextUtils.isEmpty(module) || TextUtils.isEmpty(key)) {
            return null;
        }
        return MiuiSettings.SettingsCloudData.getCloudDataString(resolver, module, key, "");
    }

    private static List<MiuiSettings.SettingsCloudData.CloudData> getCloudDataList(ContentResolver resolver, String module) {
        if (TextUtils.isEmpty(module)) {
            return null;
        }
        return MiuiSettings.SettingsCloudData.getCloudDataList(resolver, module);
    }

    private static void updateFloatWhitelist(Context context, String data) {
        List<String> pkgList = jsonArray2List(createJSONArray(data));
        if (pkgList != null && !pkgList.isEmpty()) {
            NotificationFilterHelper.updateFloatWhiteList(context, pkgList);
        }
    }

    private static void updateKeyguardWhitelist(Context context, String data) {
        List<String> pkgList = jsonArray2List(createJSONArray(data));
        if (pkgList != null && !pkgList.isEmpty()) {
            NotificationFilterHelper.updateKeyguardWhitelist(context, pkgList);
        }
    }

    private static void updateLocalAlgoModel(List<MiuiSettings.SettingsCloudData.CloudData> dataList) {
        HashMap<String, List<LocalScoreRule>> rules = new HashMap<>();
        for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
            String pkg = data.getString("pkg", "");
            if (!TextUtils.isEmpty(pkg)) {
                LocalScoreRule scoreRule = new LocalScoreRule();
                scoreRule.title = data.getString("title", "");
                scoreRule.desc = data.getString("desc", "");
                scoreRule.score = data.getInt("score", -1);
                if (rules.containsKey(pkg)) {
                    rules.get(pkg).add(scoreRule);
                } else {
                    List<LocalScoreRule> scoreRules = new ArrayList<>();
                    scoreRules.add(scoreRule);
                    rules.put(pkg, scoreRules);
                }
            }
        }
        LocalAlgoModel.updateRules(rules);
    }

    private static JSONArray createJSONArray(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            Log.d("CloudDataHelper", "createJSONArray exception json=" + json);
            return null;
        }
    }

    private static List<String> jsonArray2List(JSONArray array) {
        List<String> result = null;
        if (array != null) {
            result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                try {
                    result.add(array.getString(i));
                } catch (JSONException e) {
                    Log.d("CloudDataHelper", "jsonArray2List exception i=" + i);
                }
            }
        }
        return result;
    }

    private static void writeHashCode(Context context, String key, int hashCode) {
        Settings.Global.putInt(context.getContentResolver(), key, hashCode);
    }

    private static int readHashCode(Context context, String key) {
        return Settings.Global.getInt(context.getContentResolver(), key, -1);
    }
}
