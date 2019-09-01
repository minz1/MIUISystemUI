package com.android.systemui;

import android.os.Build;
import android.os.SystemProperties;
import java.io.File;
import miui.util.FeatureParser;

public class Constants {
    public static final String AUTOGROUP_KEY = (Build.VERSION.SDK_INT >= 26 ? "ranker_group" : "ranker_bundle");
    public static final boolean DEBUG = SystemProperties.getBoolean("debug.miuisystemui.staging", false);
    public static final boolean ENABLE_USER_FOLD = (!miui.os.Build.IS_INTERNATIONAL_BUILD);
    public static final boolean FORCE_SHOW_PROGRESS_BAR_WITH_SUBTEXT;
    public static final boolean HAS_POWER_CENTER = (!miui.os.Build.IS_TABLET);
    public static final String HOME_LAUCNHER_PACKAGE_NAME = SystemProperties.get("ro.miui.product.home", "com.miui.home");
    public static final boolean IS_CUST_SINGLE_SIM = (SystemProperties.getInt("ro.miui.singlesim", 0) == 1);
    public static final boolean IS_INTERNATIONAL = miui.os.Build.IS_INTERNATIONAL_BUILD;
    public static final boolean IS_MIPAD_CLOVER = "clover".equals(miui.os.Build.DEVICE);
    public static final boolean IS_NARROW_NOTCH = ("sakura".equals(miui.os.Build.DEVICE) || "sakura_india".equals(miui.os.Build.DEVICE) || "platina".equals(miui.os.Build.DEVICE));
    public static final boolean IS_NOTCH = (SystemProperties.getInt("ro.miui.notch", 0) == 1);
    public static final boolean IS_OLED_SCREEN = "oled".equals(SystemProperties.get("ro.display.type"));
    public static final boolean IS_TABLET = miui.os.Build.IS_TABLET;
    public static final boolean SHOW_NOTIFICATION_HEADER = (miui.os.Build.IS_INTERNATIONAL_BUILD && Build.VERSION.SDK_INT > 23);
    public static final File SOUND_CHARGE_WIRELESS = new File("/system/media/audio/ui/charge_wireless.ogg");
    public static final File SOUND_CHARGING = new File("/system/media/audio/ui/charging.ogg");
    public static final File SOUND_DISCONNECT = new File("/system/media/audio/ui/disconnect.ogg");
    public static final File SOUND_FLASHLIGHT = new File("/system/media/audio/ui/flashlight.ogg");
    public static final File SOUND_SCREENSHOT = new File("/system/media/audio/ui/screenshot.ogg");
    public static final File SOUND_SCREENSHOT_KR = new File("/system/media/audio/ui/screenshot_kr.ogg");
    public static final boolean SUPPORT_ANDROID_FLASHLIGHT = FeatureParser.getBoolean("support_android_flashlight", false);
    public static final boolean SUPPORT_AOD = FeatureParser.getBoolean("support_aod", false);
    public static final boolean SUPPORT_DISABLE_USB_BY_SIM = (miui.os.Build.IS_CM_CUSTOMIZATION_TEST || miui.os.Build.IS_CM_CUSTOMIZATION);
    public static final boolean SUPPORT_DUAL_GPS = FeatureParser.getBoolean("support_dual_gps", false);
    public static final boolean SUPPORT_EXTREME_BATTERY_SAVER = FeatureParser.getBoolean("support_extreme_battery_saver", false);
    public static final boolean SUPPORT_SCREEN_PAPER_MODE = FeatureParser.getBoolean("support_screen_paper_mode", false);
    public static final File THEME_FILE = new File("/data/system/theme/com.android.systemui");

    static {
        boolean z = false;
        if (Build.VERSION.SDK_INT == 23) {
            z = true;
        }
        FORCE_SHOW_PROGRESS_BAR_WITH_SUBTEXT = z;
    }
}
