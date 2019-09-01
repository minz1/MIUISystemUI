package com.xiaomi.analytics.internal.util;

public class ALog {
    public static boolean sEnable = false;

    public static String addPrefix(String tag) {
        return "Analytics-Api-" + tag;
    }
}
