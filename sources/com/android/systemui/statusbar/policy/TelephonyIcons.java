package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.MCCUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileSignalController;
import miui.os.Build;

public class TelephonyIcons {
    static final MobileSignalController.MobileIconGroup CARRIER_NETWORK_CHANGE;
    static final MobileSignalController.MobileIconGroup DATA_DISABLED;
    static final int[][] DATA_SIGNAL_STRENGTH = TELEPHONY_SIGNAL_STRENGTH;
    static final MobileSignalController.MobileIconGroup E;
    static final MobileSignalController.MobileIconGroup FOUR_G;
    static final MobileSignalController.MobileIconGroup FOUR_G_PLUS;
    static final MobileSignalController.MobileIconGroup G;
    static final MobileSignalController.MobileIconGroup H;
    static final MobileSignalController.MobileIconGroup LTE;
    static final MobileSignalController.MobileIconGroup LTE_PLUS;
    static final MobileSignalController.MobileIconGroup ONE_X;
    public static final int[][] TELEPHONY_SIGNAL_STRENGTH = {new int[]{R.drawable.stat_sys_signal_0, R.drawable.stat_sys_signal_1, R.drawable.stat_sys_signal_2, R.drawable.stat_sys_signal_3, R.drawable.stat_sys_signal_4, R.drawable.stat_sys_signal_5}};
    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING = {new int[]{R.drawable.stat_sys_signal_0, R.drawable.stat_sys_signal_1, R.drawable.stat_sys_signal_2, R.drawable.stat_sys_signal_3, R.drawable.stat_sys_signal_4, R.drawable.stat_sys_signal_5}};
    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_R = {new int[]{R.drawable.stat_sys_signal_0_default_roam, R.drawable.stat_sys_signal_1_default_roam, R.drawable.stat_sys_signal_2_default_roam, R.drawable.stat_sys_signal_3_default_roam, R.drawable.stat_sys_signal_4_default_roam}, new int[]{R.drawable.stat_sys_signal_0_default_fully_roam, R.drawable.stat_sys_signal_1_default_fully_roam, R.drawable.stat_sys_signal_2_default_fully_roam, R.drawable.stat_sys_signal_3_default_fully_roam, R.drawable.stat_sys_signal_4_default_fully_roam}};
    static final MobileSignalController.MobileIconGroup THREE_G;
    static final MobileSignalController.MobileIconGroup UNKNOWN;
    static final MobileSignalController.MobileIconGroup WFC;
    private static boolean isInitiated = false;
    static String[] mDataActivityArray;
    static String[] mDataTypeArray;
    static String[] mDataTypeDescriptionArray;
    static String[] mDataTypeGenerationArray;
    static String[] mDataTypeGenerationDescArray;
    static SparseArray<String> mDataTypeNameCusRegMap = new SparseArray<>();
    static String[] mDataTypeNameDefault;
    static SparseArray<String> mDataTypeNameMIUIRegion = new SparseArray<>();
    static SparseArray<SparseArray<String>> mDataTypeNameMcc = new SparseArray<>();
    private static Resources mRes;
    static int[] mSelectedDataActivityIndex;
    static String[] mSelectedDataTypeDesc;
    static int[] mSelectedDataTypeIcon;
    static int[] mSelectedQSDataTypeIcon;
    static int[] mSelectedSignalStreagthIndex;
    static String[] mSignalNullArray;
    static String[] mSignalStrengthArray;
    static String[] mSignalStrengthDesc;
    static String[] mSignalStrengthRoamingArray;
    static SparseArray<Integer> mStacked2SingleIconLookup;

    static {
        mDataTypeNameMcc.put(0, new SparseArray());
        mDataTypeNameMcc.put(1, new SparseArray());
        MobileSignalController.MobileIconGroup mobileIconGroup = new MobileSignalController.MobileIconGroup("CARRIER_NETWORK_CHANGE", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_carrier_network_change_mode, 0, false, 0);
        CARRIER_NETWORK_CHANGE = mobileIconGroup;
        MobileSignalController.MobileIconGroup mobileIconGroup2 = new MobileSignalController.MobileIconGroup("3G", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_3g, R.drawable.stat_sys_data_fully_connected_3g, true, R.drawable.ic_qs_signal_3g);
        THREE_G = mobileIconGroup2;
        MobileSignalController.MobileIconGroup mobileIconGroup3 = new MobileSignalController.MobileIconGroup("WFC", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], 0, 0, false, 0);
        WFC = mobileIconGroup3;
        MobileSignalController.MobileIconGroup mobileIconGroup4 = new MobileSignalController.MobileIconGroup("Unknown", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], 0, 0, false, 0);
        UNKNOWN = mobileIconGroup4;
        MobileSignalController.MobileIconGroup mobileIconGroup5 = new MobileSignalController.MobileIconGroup("E", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_edge, R.drawable.stat_sys_data_fully_connected_e, false, R.drawable.ic_qs_signal_e);
        E = mobileIconGroup5;
        MobileSignalController.MobileIconGroup mobileIconGroup6 = new MobileSignalController.MobileIconGroup("1X", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_cdma, R.drawable.stat_sys_data_fully_connected_1x, true, R.drawable.ic_qs_signal_1x);
        ONE_X = mobileIconGroup6;
        MobileSignalController.MobileIconGroup mobileIconGroup7 = new MobileSignalController.MobileIconGroup("G", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_gprs, R.drawable.stat_sys_data_fully_connected_g, false, R.drawable.ic_qs_signal_g);
        G = mobileIconGroup7;
        MobileSignalController.MobileIconGroup mobileIconGroup8 = new MobileSignalController.MobileIconGroup("H", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.f0accessibility_data_connection_35g, R.drawable.stat_sys_data_fully_connected_h, false, R.drawable.ic_qs_signal_h);
        H = mobileIconGroup8;
        MobileSignalController.MobileIconGroup mobileIconGroup9 = new MobileSignalController.MobileIconGroup("4G", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_4g, R.drawable.stat_sys_data_fully_connected_4g, true, R.drawable.ic_qs_signal_4g);
        FOUR_G = mobileIconGroup9;
        MobileSignalController.MobileIconGroup mobileIconGroup10 = new MobileSignalController.MobileIconGroup("4G+", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_4g_plus, R.drawable.stat_sys_data_fully_connected_4g_plus, true, R.drawable.ic_qs_signal_4g_plus);
        FOUR_G_PLUS = mobileIconGroup10;
        MobileSignalController.MobileIconGroup mobileIconGroup11 = new MobileSignalController.MobileIconGroup("LTE", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_lte, R.drawable.stat_sys_data_fully_connected_lte, true, R.drawable.ic_qs_signal_lte);
        LTE = mobileIconGroup11;
        MobileSignalController.MobileIconGroup mobileIconGroup12 = new MobileSignalController.MobileIconGroup("LTE+", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_data_connection_lte_plus, R.drawable.stat_sys_data_fully_connected_lte_plus, true, R.drawable.ic_qs_signal_lte_plus);
        LTE_PLUS = mobileIconGroup12;
        MobileSignalController.MobileIconGroup mobileIconGroup13 = new MobileSignalController.MobileIconGroup("DataDisabled", null, null, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH, 0, 0, 0, 0, AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0], R.string.accessibility_cell_data_off, R.drawable.stat_sys_data_disabled, false, R.drawable.ic_data_disabled);
        DATA_DISABLED = mobileIconGroup13;
    }

    public static void initDataTypeName(Context context) {
        setDataTypeDefault(context);
        setDataTypeCusomizedRegion(context);
        updateDataTypeMiuiRegion(context, System.getProperty("ro.miui.mcc"));
    }

    public static void setDataTypeDefault(Context context) {
        if (mDataTypeNameDefault == null) {
            mDataTypeNameDefault = context.getResources().getStringArray(R.array.data_type_name_default);
        }
    }

    public static void setDataTypeCusomizedRegion(Context context) {
        int[] cus_key = context.getResources().getIntArray(R.array.data_type_name_cus_reg_key);
        String[] cus_val = context.getResources().getStringArray(R.array.data_type_name_cus_reg_value);
        for (int i = 0; i < cus_key.length; i++) {
            mDataTypeNameCusRegMap.put(cus_key[i], cus_val[i]);
        }
    }

    public static void updateDataTypeMcc(Context context, String operation, int slot) {
        SparseArray<String> dataTypeNameMcc = mDataTypeNameMcc.get(slot);
        dataTypeNameMcc.clear();
        Resources resources = MCCUtils.getResourcesForOperation(context, operation, false);
        int[] cus_key = resources.getIntArray(R.array.data_type_name_mcc_key);
        String[] cus_val = resources.getStringArray(R.array.data_type_name_mcc_value);
        for (int i = 0; i < cus_key.length; i++) {
            dataTypeNameMcc.put(cus_key[i], cus_val[i]);
        }
    }

    public static void updateDataTypeMiuiRegion(Context context, String miuiMcc) {
        mDataTypeNameMIUIRegion.clear();
        String mcc = miuiMcc;
        if (!TextUtils.isEmpty(miuiMcc)) {
            mcc = miuiMcc.substring(1, miuiMcc.length());
        }
        Resources resources = MCCUtils.getResourcesForOperation(context, mcc, false);
        int[] cus_key = resources.getIntArray(R.array.data_type_name_miui_mcc_key);
        String[] cus_val = resources.getStringArray(R.array.data_type_name_miui_mcc_value);
        for (int i = 0; i < cus_key.length; i++) {
            mDataTypeNameMIUIRegion.put(cus_key[i], cus_val[i]);
        }
    }

    public static String getNetworkTypeName(int type, int slot) {
        if (mDataTypeNameCusRegMap.size() > 0) {
            if (mDataTypeNameCusRegMap.get(type) != null) {
                return mDataTypeNameCusRegMap.get(type);
            }
        } else if (mDataTypeNameMcc.get(slot).size() > 0) {
            String typeName = (String) mDataTypeNameMcc.get(slot).get(type);
            if (typeName != null) {
                return typeName;
            }
        } else if (mDataTypeNameMIUIRegion.size() > 0) {
            String typeName2 = mDataTypeNameMIUIRegion.get(type);
            if (typeName2 != null) {
                return typeName2;
            }
        } else if (Build.IS_CM_CUSTOMIZATION_TEST && type == 1) {
            return "2G";
        }
        if (type < 0 || mDataTypeNameDefault == null || type >= mDataTypeNameDefault.length) {
            return "";
        }
        return mDataTypeNameDefault[type];
    }

    static void readIconsFromXml(Context context) {
        if (isInitiated) {
            log("TelephonyIcons", "readIconsFromXml, already read!");
            return;
        }
        mRes = context.getResources();
        try {
            mDataTypeArray = mRes.getStringArray(R.array.multi_data_type);
            mDataTypeDescriptionArray = mRes.getStringArray(R.array.telephony_data_type_description);
            mDataTypeGenerationArray = mRes.getStringArray(R.array.telephony_data_type_generation);
            mDataTypeGenerationDescArray = mRes.getStringArray(R.array.telephony_data_type_generation_description);
            mDataActivityArray = mRes.getStringArray(R.array.multi_data_activity);
            mSignalStrengthArray = mRes.getStringArray(R.array.multi_signal_strength);
            mSignalStrengthRoamingArray = mRes.getStringArray(R.array.multi_signal_strength_roaming);
            mSignalNullArray = mRes.getStringArray(R.array.multi_signal_null);
            mSignalStrengthDesc = mRes.getStringArray(R.array.signal_strength_description);
            initStacked2SingleIconLookup();
            if (mSelectedDataTypeIcon == null && mDataTypeArray.length != 0) {
                mSelectedDataTypeIcon = new int[mDataTypeArray.length];
            }
            if (mSelectedQSDataTypeIcon == null && mDataTypeArray.length != 0) {
                mSelectedQSDataTypeIcon = new int[mDataTypeArray.length];
            }
            if (mSelectedDataTypeDesc == null && mDataTypeArray.length != 0) {
                mSelectedDataTypeDesc = new String[mDataTypeArray.length];
            }
            if (mSelectedDataActivityIndex == null && mDataActivityArray.length != 0) {
                mSelectedDataActivityIndex = new int[mDataActivityArray.length];
            }
            if (mSelectedSignalStreagthIndex == null && mSignalStrengthArray.length != 0) {
                mSelectedSignalStreagthIndex = new int[mSignalStrengthArray.length];
            }
            isInitiated = true;
        } catch (Resources.NotFoundException e) {
            isInitiated = false;
            log("TelephonyIcons", "readIconsFromXml, exception happened: " + e);
        }
    }

    static void initStacked2SingleIconLookup() {
        mStacked2SingleIconLookup = new SparseArray<>();
        TypedArray stackedIcons = mRes.obtainTypedArray(R.array.stacked_signal_icons);
        TypedArray singleIcons = mRes.obtainTypedArray(R.array.single_signal_icons);
        mStacked2SingleIconLookup.clear();
        int i = 0;
        while (i < stackedIcons.length() && i < singleIcons.length()) {
            mStacked2SingleIconLookup.put(stackedIcons.getResourceId(i, 0), Integer.valueOf(singleIcons.getResourceId(i, 0)));
            i++;
        }
        stackedIcons.recycle();
        singleIcons.recycle();
        log("TelephonyIcons", "initStacked2SingleIconLookup: size=" + mStacked2SingleIconLookup.size());
    }

    static int getSignalNullIcon(int slot) {
        if (mSignalNullArray == null) {
            return 0;
        }
        String resName = mSignalNullArray[slot];
        log("TelephonyIcons", "null signal icon name: " + resName);
        return mRes.getIdentifier(resName, null, "com.android.systemui");
    }

    static int getQSDataTypeIcon(int slot) {
        return mSelectedQSDataTypeIcon[slot];
    }

    static int getDataTypeIcon(int slot) {
        log("TelephonyIcons", "getDataTypeIcon " + String.format("sub=%d", new Object[]{Integer.valueOf(slot)}));
        return mSelectedDataTypeIcon[slot];
    }

    static int getDataTypeDesc(int slot) {
        return mRes.getIdentifier(mSelectedDataTypeDesc[slot], null, "com.android.systemui");
    }

    static int getDataActivity(int slot, int activity) {
        log("TelephonyIcons", String.format("getDataActivity, slot=%d, activity=%d", new Object[]{Integer.valueOf(slot), Integer.valueOf(activity)}));
        return mRes.getIdentifier(mRes.getStringArray(mRes.getIdentifier(mRes.getStringArray(mRes.getIdentifier(mDataActivityArray[slot], null, "com.android.systemui"))[mSelectedDataActivityIndex[slot]], null, "com.android.systemui"))[activity], null, "com.android.systemui");
    }

    static int getSignalStrengthIcon(int slot, int inet, int level, boolean roaming) {
        log("TelephonyIcons", "getSignalStrengthIcon: " + String.format("slot=%d, inetCondition=%d, level=%d, roaming=%b", new Object[]{Integer.valueOf(slot), Integer.valueOf(inet), Integer.valueOf(level), Boolean.valueOf(roaming)}));
        return TELEPHONY_SIGNAL_STRENGTH[0][level];
    }

    static int convertMobileStrengthIcon(int stackedIcon) {
        if (mStacked2SingleIconLookup != null && mStacked2SingleIconLookup.indexOfKey(stackedIcon) >= 0) {
            return mStacked2SingleIconLookup.get(stackedIcon).intValue();
        }
        return stackedIcon;
    }

    static int getStackedVoiceIcon(int level) {
        switch (level) {
            case 0:
                return R.drawable.stat_sys_signal_0_2g;
            case 1:
                return R.drawable.stat_sys_signal_1_2g;
            case 2:
                return R.drawable.stat_sys_signal_2_2g;
            case 3:
                return R.drawable.stat_sys_signal_3_2g;
            case 4:
                return R.drawable.stat_sys_signal_4_2g;
            default:
                return 0;
        }
    }

    static int getRoamingSignalIconId(int level, int inet) {
        return TELEPHONY_SIGNAL_STRENGTH_ROAMING_R[inet][level];
    }

    static int[] getSignalStrengthDes(int slot) {
        int[] resId = new int[5];
        for (int i = 0; i < 5; i++) {
            resId[i] = mRes.getIdentifier(mSignalStrengthDesc[i], null, "com.android.systemui");
        }
        return resId;
    }

    private static void log(String tag, String str) {
        Log.d(tag, str);
    }
}
