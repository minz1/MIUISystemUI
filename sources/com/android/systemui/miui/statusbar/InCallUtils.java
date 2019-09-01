package com.android.systemui.miui.statusbar;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.miui.voiptalk.service.MiuiVoipManager;
import miui.telephony.TelephonyManager;

public class InCallUtils {
    public static boolean isInCallNotification(Context context, ExpandedNotification notification) {
        return isPhoneInCallNotification(notification) || isVoipInCallNotification(context, notification);
    }

    public static boolean isPhoneInCallNotificationInVideo(ExpandedNotification notification) {
        return isPhoneInCallNotification(notification) && notification.getNotification().extras.getBoolean("hasVideoCall");
    }

    public static boolean isPhoneInCallNotification(ExpandedNotification notification) {
        boolean z = false;
        if (notification == null) {
            return false;
        }
        if ((notification.getBasePkg().equals("com.android.incallui") || notification.getBasePkg().equals("com.android.phone")) && "incall".equals(notification.getTag()) && TelephonyManager.getDefault().getCallState() == 1) {
            z = true;
        }
        return z;
    }

    public static boolean isVoipInCallNotification(Context context, ExpandedNotification notification) {
        boolean z = false;
        if (notification == null) {
            return false;
        }
        if (notification.getBasePkg().equals("com.miui.voip") && "voip_incall".equals(notification.getTag()) && MiuiVoipManager.getInstance(context).getCallState() == 1) {
            z = true;
        }
        return z;
    }

    public static void goInCallScreen(Context context) {
        goInCallScreen(context, null);
    }

    public static void goInCallScreen(Context context, Bundle extras) {
        Intent intent = new Intent("android.intent.action.MAIN");
        if (extras != null) {
            intent.putExtras(extras);
        }
        intent.setFlags(277086208);
        if (TelephonyManager.getDefault().getCallState() != 0) {
            intent.setClassName("com.android.incallui", "com.android.incallui.InCallActivity");
        } else {
            intent.setClassName("com.miui.voip", "com.miui.voiptalk.activity.VoipCallActivity");
        }
        try {
            context.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (ActivityNotFoundException e) {
        }
    }

    public static boolean isInCallNotificationHeadsUp(Context context, HeadsUpManager headsUpManager) {
        HeadsUpManager.HeadsUpEntry topEntry = headsUpManager.getTopEntry();
        return topEntry != null && topEntry.entry.row.isHeadsUp() && topEntry.entry.row.isPinned() && isInCallNotification(context, topEntry.entry.notification);
    }

    public static boolean isInCallScreenShowing(Context context) {
        ComponentName topActivity = Util.getTopActivity(context);
        String runningActivity = topActivity == null ? null : topActivity.getClassName();
        boolean z = false;
        if ("com.android.phone.MiuiInCallScreen".equals(runningActivity) || "com.android.incallui.InCallActivity".equals(runningActivity)) {
            if (TelephonyManager.getDefault().getCallState() == 1) {
                z = true;
            }
            return z;
        } else if (!"com.miui.voiptalk.activity.VoipCallActivity".equals(runningActivity)) {
            return false;
        } else {
            if (MiuiVoipManager.getInstance(context).getCallState() == 1) {
                z = true;
            }
            return z;
        }
    }

    public static boolean isCallScreenShowing(Context context) {
        ComponentName topActivity = Util.getTopActivity(context);
        String runningActivity = topActivity == null ? null : topActivity.getClassName();
        if ("com.miui.voiptalk.activity.VoipCallActivity".equals(runningActivity) || "com.android.phone.MiuiInCallScreen".equals(runningActivity) || "com.android.incallui.InCallActivity".equals(runningActivity)) {
            return true;
        }
        return false;
    }

    public static boolean needShowReturnToInVoipCallScreenButton(Context context) {
        return MiuiVoipManager.getInstance(context).isVoipCallUiOnBack() && !isCallScreenShowing(context) && TelephonyManager.getDefault().getCallState() == 0 && MiuiVoipManager.getInstance(context).getCallState() != 0;
    }
}
