package com.android.keyguard.charge;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ChargeUtils {
    private static String KEY_BATTERY_ENDURANCE_TIME = "battery_endurance_time";
    private static String KEY_LEFT_CHARGE_TIME = "left_charge_time";
    private static String KEY_QUICK_CHARGE = "quick_charge";
    private static String METHOD_GET_BATTERY_INFO = "getBatteryInfo";
    private static String METHOD_GET_POWER_SUPPLY_INFO = "getPowerSupplyInfo";
    private static String PROVIDER_POWER_CENTER = "content://com.miui.powercenter.provider";

    /* JADX WARNING: Removed duplicated region for block: B:34:0x00b1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String getChargingHintText(android.content.Context r19, boolean r20, int r21) {
        /*
            r0 = r21
            if (r20 != 0) goto L_0x0006
            r1 = 0
            return r1
        L_0x0006:
            com.android.keyguard.KeyguardUpdateMonitor r1 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r19)
            boolean r1 = r1.isNeedRepositionDevice()
            boolean r2 = com.android.keyguard.MiuiKeyguardUtils.supportWirelessCharge()
            if (r2 == 0) goto L_0x0020
            if (r1 == 0) goto L_0x0020
            r2 = 2131822527(0x7f1107bf, float:1.9277828E38)
            r3 = r19
            java.lang.String r2 = r3.getString(r2)
            return r2
        L_0x0020:
            r3 = r19
            r2 = 0
            android.os.Bundle r4 = getBatteryInfo(r19)
            android.content.res.Resources r5 = r19.getResources()
            r7 = 100
            if (r4 == 0) goto L_0x00a0
            if (r0 != r7) goto L_0x0034
            java.lang.String r8 = KEY_BATTERY_ENDURANCE_TIME
            goto L_0x0036
        L_0x0034:
            java.lang.String r8 = KEY_LEFT_CHARGE_TIME
        L_0x0036:
            long r8 = r4.getLong(r8)
            long r10 = getHours(r8)
            long r12 = getMins(r8)
            if (r0 != r7) goto L_0x0093
            r14 = 0
            int r16 = (r10 > r14 ? 1 : (r10 == r14 ? 0 : -1))
            r7 = 1
            r17 = 0
            if (r16 <= 0) goto L_0x0069
            int r16 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1))
            if (r16 <= 0) goto L_0x0069
            int r6 = (int) r10
            r14 = 2
            java.lang.Object[] r14 = new java.lang.Object[r14]
            java.lang.Long r15 = java.lang.Long.valueOf(r10)
            r14[r17] = r15
            java.lang.Long r15 = java.lang.Long.valueOf(r12)
            r14[r7] = r15
            r7 = 2131689484(0x7f0f000c, float:1.9007985E38)
            java.lang.String r2 = r5.getQuantityString(r7, r6, r14)
            goto L_0x00a0
        L_0x0069:
            int r6 = (r10 > r14 ? 1 : (r10 == r14 ? 0 : -1))
            if (r6 <= 0) goto L_0x007e
            r6 = 2131689482(0x7f0f000a, float:1.900798E38)
            int r14 = (int) r10
            java.lang.Object[] r7 = new java.lang.Object[r7]
            java.lang.Long r15 = java.lang.Long.valueOf(r10)
            r7[r17] = r15
            java.lang.String r2 = r5.getQuantityString(r6, r14, r7)
            goto L_0x00a0
        L_0x007e:
            int r6 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1))
            if (r6 <= 0) goto L_0x00a0
            r6 = 2131689483(0x7f0f000b, float:1.9007983E38)
            int r14 = (int) r12
            java.lang.Object[] r7 = new java.lang.Object[r7]
            java.lang.Long r15 = java.lang.Long.valueOf(r12)
            r7[r17] = r15
            java.lang.String r2 = r5.getQuantityString(r6, r14, r7)
            goto L_0x00a0
        L_0x0093:
            boolean r6 = isQuickCharging(r19)
            if (r6 == 0) goto L_0x00a4
            r6 = 2131821506(0x7f1103c2, float:1.9275757E38)
            java.lang.String r2 = r5.getString(r6)
        L_0x00a0:
            r6 = 2131821505(0x7f1103c1, float:1.9275755E38)
            goto L_0x00ab
        L_0x00a4:
            r6 = 2131821505(0x7f1103c1, float:1.9275755E38)
            java.lang.String r2 = r5.getString(r6)
        L_0x00ab:
            boolean r7 = android.text.TextUtils.isEmpty(r2)
            if (r7 == 0) goto L_0x00bd
            r7 = 100
            if (r0 != r7) goto L_0x00b9
            r6 = 2131821458(0x7f110392, float:1.927566E38)
        L_0x00b9:
            java.lang.String r2 = r5.getString(r6)
        L_0x00bd:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.charge.ChargeUtils.getChargingHintText(android.content.Context, boolean, int):java.lang.String");
    }

    private static Bundle getBatteryInfo(Context context) {
        try {
            return context.getContentResolver().call(Uri.parse(PROVIDER_POWER_CENTER), METHOD_GET_BATTERY_INFO, null, null);
        } catch (Exception e) {
            Log.e("ChargeUtils", "cannot find the path getBatteryInfo of content://com.miui.powercenter.provider");
            return null;
        }
    }

    public static boolean isQuickCharging(Context context) {
        try {
            return context.getContentResolver().call(Uri.parse(PROVIDER_POWER_CENTER), METHOD_GET_POWER_SUPPLY_INFO, null, null).getBoolean(KEY_QUICK_CHARGE);
        } catch (Exception e) {
            Log.e("ChargeUtils", "cannot find the path getPowerSupplyInfo of content://com.miui.powercenter.provider");
            return false;
        }
    }

    public static boolean isWirelessCarMode(int deviceNum) {
        return deviceNum == 11;
    }

    public static boolean isWirelessSuperRapidCharge(int deviceNum) {
        return deviceNum == 9 || deviceNum == 10 || deviceNum == 11;
    }

    public static boolean isSuperRapidCharge(int deviceNum) {
        return deviceNum == 2;
    }

    public static boolean isRapidCharge(int deviceNum) {
        return deviceNum == 1;
    }

    public static long getHours(long time) {
        return time / 3600000;
    }

    public static long getMins(long time) {
        return (time % 3600000) / 60000;
    }
}
