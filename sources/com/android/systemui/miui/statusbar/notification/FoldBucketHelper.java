package com.android.systemui.miui.statusbar.notification;

import android.os.SystemProperties;
import android.util.Log;
import com.android.systemui.Constants;
import java.util.Random;

public class FoldBucketHelper {
    private static int sFoldBucket;

    public static void init() {
        if (shouldUpdateRankNum()) {
            if (Constants.IS_INTERNATIONAL) {
                sFoldBucket = 0;
            } else {
                int percent = new Random().nextInt(100);
                if (percent < 90) {
                    sFoldBucket = 3;
                } else {
                    sFoldBucket = 1;
                }
                Log.d("FoldBucketHelper", "percent=" + percent + ",sRandomFoldType=" + sFoldBucket);
            }
            SystemProperties.set("persist.sys.notification_rank", String.valueOf(sFoldBucket));
            SystemProperties.set("persist.sys.notification_ver", "1");
            return;
        }
        try {
            sFoldBucket = Integer.parseInt(SystemProperties.get("persist.sys.notification_rank", ""));
        } catch (NumberFormatException e) {
            sFoldBucket = 0;
        }
        if (sFoldBucket < 0 || sFoldBucket > 3 || (Constants.IS_INTERNATIONAL && sFoldBucket != 0)) {
            sFoldBucket = 0;
            SystemProperties.set("persist.sys.notification_rank", String.valueOf(sFoldBucket));
        }
    }

    public static boolean allowFold() {
        return sFoldBucket == 3;
    }

    public static int getFoldBucket() {
        return sFoldBucket;
    }

    private static boolean shouldUpdateRankNum() {
        if (!"1".equals(SystemProperties.get("persist.sys.notification_ver", ""))) {
            return true;
        }
        return false;
    }
}
