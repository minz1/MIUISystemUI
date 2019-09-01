package com.android.systemui.miui.statusbar.phone.rank;

import android.os.Build;
import android.text.TextUtils;
import com.android.systemui.SystemUICompat;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RankUtil {
    private static final boolean SUPPORT_HIGH_PRIORITY = (Build.VERSION.SDK_INT < 26);
    public static int UNFLOD_LIMIT = 3;
    public static long sGap = 7200000;
    private static HashMap<String, Boolean> sHighPriorityMap = new HashMap<>();
    private static List<String> sIMPackages = new ArrayList();
    private static long sLastNotificationAddedTime = System.currentTimeMillis();
    public static long sNewNotification = 10000;

    static {
        sIMPackages.add("com.android.mms");
        sIMPackages.add("com.tencent.mobileqq");
        sIMPackages.add("com.tencent.mm");
    }

    public static int compareHeadsUp(NotificationData.Entry a, NotificationData.Entry b, HeadsUpManager headsUpManager) {
        boolean isHeadsUp = a.row.isHeadsUp();
        if (isHeadsUp != b.row.isHeadsUp()) {
            return isHeadsUp ? -1 : 1;
        } else if (isHeadsUp) {
            return headsUpManager.compare(a, b);
        } else {
            return 0;
        }
    }

    public static int compareHighPriority(NotificationData.Entry a, NotificationData.Entry b) {
        if (!SUPPORT_HIGH_PRIORITY) {
            return 0;
        }
        boolean aHighPriority = isHighPriority(a.notification.getPackageName(), a.notification.getUid());
        if (aHighPriority == isHighPriority(b.notification.getPackageName(), b.notification.getUid())) {
            return 0;
        }
        return aHighPriority ? -1 : 1;
    }

    private static boolean isHighPriority(String pkg, int uid) {
        if (!sHighPriorityMap.containsKey(pkg)) {
            updateHighPriorityMap(pkg, uid);
        }
        return sHighPriorityMap.get(pkg).booleanValue();
    }

    public static void updateHighPriorityMap(String pkg, int uid) {
        if (SUPPORT_HIGH_PRIORITY) {
            try {
                sHighPriorityMap.put(pkg, Boolean.valueOf(SystemUICompat.isHighPriority(pkg, uid)));
            } catch (Exception e) {
                sHighPriorityMap.remove(pkg);
            }
        }
    }

    public static int compareNew(NotificationData.Entry a, NotificationData.Entry b) {
        boolean aNew = isNewNotification(a);
        if (aNew == isNewNotification(b)) {
            return 0;
        }
        return aNew ? -1 : 1;
    }

    public static int compareIM(NotificationData.Entry a, NotificationData.Entry b) {
        boolean aIM = isIMNotification(a);
        if (aIM == isIMNotification(b)) {
            return 0;
        }
        return aIM ? -1 : 1;
    }

    public static int compareMedia(NotificationData.Entry a, NotificationData.Entry b, int aImportance, int bImportance, String activeMediaNotificationKey) {
        int i = 1;
        boolean aMedia = a.key.equals(activeMediaNotificationKey) && aImportance > 1;
        if (aMedia == (b.key.equals(activeMediaNotificationKey) && bImportance > 1)) {
            return 0;
        }
        if (aMedia) {
            i = -1;
        }
        return i;
    }

    public static int compareSystemMax(NotificationData.Entry a, NotificationData.Entry b, int aImportance, int bImportance) {
        boolean aSystemMax = isSystemMaxImportanceNotification(a, aImportance);
        if (aSystemMax == isSystemMaxImportanceNotification(b, bImportance)) {
            return 0;
        }
        return aSystemMax ? -1 : 1;
    }

    public static int compareImportance(int aImportance, int bImportance) {
        return Integer.compare(bImportance, aImportance);
    }

    public static int comparePriority(NotificationData.Entry a, NotificationData.Entry b) {
        return Integer.compare(b.notification.getNotification().priority, a.notification.getNotification().priority);
    }

    private static boolean isSystemMaxImportanceNotification(NotificationData.Entry entry, int importance) {
        return entry.notification != null && importance >= 4 && NotificationUtil.isSystemNotification(entry.notification);
    }

    private static boolean isIMNotification(NotificationData.Entry entry) {
        return entry.notification != null && !TextUtils.isEmpty(entry.notification.getPackageName()) && sIMPackages.contains(entry.notification.getPackageName());
    }

    private static boolean isNewNotification(NotificationData.Entry entry) {
        return sLastNotificationAddedTime - entry.notification.getNotification().when < sNewNotification;
    }

    public static void updateLastNotificationAddedTime() {
        sLastNotificationAddedTime = System.currentTimeMillis();
    }
}
