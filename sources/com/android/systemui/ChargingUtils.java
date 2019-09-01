package com.android.systemui;

import android.content.Context;
import android.net.Uri;
import android.util.Slog;

public class ChargingUtils {
    public static boolean isQuickCharging(Context context) {
        boolean quickCharging = false;
        if (!Constants.HAS_POWER_CENTER) {
            Slog.d("ChargingUtils", "isQuickCharging() quickCharging=" + false + " (not has power center).");
            return false;
        }
        try {
            quickCharging = context.getContentResolver().call(Uri.parse("content://com.miui.powercenter.provider"), "getPowerSupplyInfo", null, null).getBoolean("quick_charge");
        } catch (Exception e) {
            Slog.d("ChargingUtils", "isQuickCharging() error: " + e);
        }
        Slog.d("ChargingUtils", "isQuickCharging() quickCharging=" + quickCharging);
        return quickCharging;
    }
}
