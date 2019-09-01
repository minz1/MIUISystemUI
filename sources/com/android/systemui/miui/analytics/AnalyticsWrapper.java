package com.android.systemui.miui.analytics;

import android.content.Context;
import com.android.systemui.Constants;
import com.xiaomi.mistatistic.sdk.CustomSettings;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import java.util.Map;
import miui.os.Build;

public class AnalyticsWrapper {
    private static String resolveChannelName() {
        if (Build.IS_ALPHA_BUILD) {
            return "MIUI10-alpha";
        }
        if (Build.IS_DEVELOPMENT_VERSION) {
            return "MIUI10-dev";
        }
        return "MIUI10";
    }

    public static void init(Context context) {
        MiStatInterface.initialize(context, "1000271", "420100086271", resolveChannelName());
        MiStatInterface.setUploadPolicy(1, 0);
        MiStatInterface.enableExceptionCatcher(false);
        boolean enableLog = Constants.DEBUG;
        CustomSettings.setUseSystemStatService(!enableLog);
        if (enableLog) {
            MiStatInterface.enableLog();
        }
    }

    public static void recordCountEvent(String category, String event) {
        MiStatInterface.recordCountEvent(category, event);
    }

    public static void recordCountEvent(String category, String event, Map<String, String> params) {
        MiStatInterface.recordCountEvent(category, event, params);
    }

    public static void recordCountEventAnonymous(String category, String event) {
        MiStatInterface.recordCountEventAnonymous(category, event);
    }

    public static void recordCountEventAnonymous(String category, String event, Map<String, String> params) {
        MiStatInterface.recordCountEventAnonymous(category, event, params);
    }

    public static void recordCalculateEvent(String category, String event, long value) {
        MiStatInterface.recordCalculateEvent(category, event, value);
    }

    public static void recordCalculateEventAnonymous(String category, String event, long value) {
        MiStatInterface.recordCalculateEventAnonymous(category, event, value);
    }

    public static void recordCalculateEventAnonymous(String category, String event, long value, Map<String, String> params) {
        MiStatInterface.recordCalculateEventAnonymous(category, event, value, params);
    }

    public static void recordStringPropertyEvent(String category, String event, String value) {
        MiStatInterface.recordStringPropertyEvent(category, event, value);
    }

    public static void recordStringPropertyEventAnonymous(String category, String event, String value) {
        MiStatInterface.recordStringPropertyEventAnonymous(category, event, value);
    }
}
