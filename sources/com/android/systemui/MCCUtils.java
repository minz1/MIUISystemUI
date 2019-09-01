package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import java.util.Arrays;

public class MCCUtils {
    public static boolean sIsIROperation;
    public static boolean sIsMXOperation;
    public static boolean sIsNPOperation;
    public static boolean sIsUSAOperation;

    public static void checkOperation(Context context, String operation) {
        if (!TextUtils.isEmpty(operation)) {
            String mcc = operation.substring(0, 3);
            sIsUSAOperation = Arrays.asList(context.getResources().getStringArray(R.array.usa_mcc)).contains(mcc);
            sIsMXOperation = context.getResources().getString(R.string.mx_mcc).equals(mcc);
            sIsIROperation = context.getResources().getString(R.string.ir_mcc).equals(mcc);
            sIsNPOperation = context.getResources().getString(R.string.np_mcc).equals(mcc);
        }
    }

    public static boolean isShowPlmnAndSpn(Context context, String operation) {
        if (TextUtils.isEmpty(operation)) {
            return false;
        }
        return getResourcesForOperation(context, operation, true).getBoolean(R.bool.show_plmn_and_spn_in_carrier);
    }

    public static Resources getResourcesForOperation(Context context, String operation, boolean invalidMnc) {
        if (TextUtils.isEmpty(operation)) {
            return context.getResources();
        }
        Configuration config = context.getResources().getConfiguration();
        Configuration newConfig = new Configuration();
        newConfig.setTo(config);
        int mnc = 0;
        int mcc = Integer.valueOf(operation.substring(0, 3)).intValue();
        if (invalidMnc) {
            mnc = Integer.valueOf(operation.substring(3, operation.length())).intValue();
        }
        newConfig.mcc = mcc;
        newConfig.mnc = mnc;
        if (newConfig.mnc == 0) {
            newConfig.mnc = 65535;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        DisplayMetrics newMetrics = new DisplayMetrics();
        newMetrics.setTo(metrics);
        return new Resources(context.getResources().getAssets(), newMetrics, newConfig);
    }
}
