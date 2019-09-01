package com.android.keyguard;

import android.content.Context;
import android.os.SystemProperties;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;

public class Utils {
    private static boolean sSystemBootCompleted;

    public static String getHourMinformat(Context context) {
        String timePattern = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toPattern().toString();
        if (timePattern.contains("a")) {
            return timePattern.replace("a", "").trim();
        }
        return timePattern;
    }

    public static boolean isBootCompleted() {
        if (!sSystemBootCompleted) {
            sSystemBootCompleted = "1".equals(SystemProperties.get("sys.boot_completed"));
        }
        return sSystemBootCompleted;
    }
}
