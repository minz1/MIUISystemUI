package com.android.keyguard;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.hardware.miuiface.IMiuiFaceManager;
import android.hardware.miuiface.MiuiFaceFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.security.FingerprintIdUtils;
import android.security.MiuiLockPatternUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.TouchDelegate;
import android.view.View;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.utils.PhoneUtils;
import com.android.keyguard.widget.AODKeys;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.miui.DrawableUtils;
import com.android.systemui.statusbar.policy.BatteryController;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import miui.graphics.BitmapFactory;
import miui.os.Build;
import miui.util.FeatureParser;

public class MiuiKeyguardUtils {
    private static final String CUSTOMIZED_REGION = SystemProperties.get("ro.miui.customized.region", "");
    private static final boolean GXZW_SENSOR = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    private static final boolean SUPPORT_WIRELESS_CHARGE = new File("/sys/class/power_supply/wireless/signal_strength").exists();
    private static IMiuiFaceManager mFaceManager = null;
    private static int sDayOfStatistics = -1;
    private static List<String> sDeviceSupportFace = new ArrayList();
    private static List<String> sDeviceSupportFaceService = new ArrayList();
    private static List<String> sDeviceSupportLiftingCamera = new ArrayList();
    private static List<String> sDeviceSupportPickupByMTK = new ArrayList();
    private static List<String> sDeviceSupportScreenOnDelay = new ArrayList();
    private static List<String> sDeviceUseCamera1 = new ArrayList();
    private static List<String> sDeviceUseCamera2 = new ArrayList();
    private static FingerprintHelper sFingerprintHelper = null;
    private static List<String> sGlobalDeviceSenseTimeSupport = new ArrayList();
    private static List<String> sGlobalRegionSenseTimeSupport = new ArrayList();
    private static boolean sHasNavigationBar;
    private static boolean sHasSetAuth;
    private static boolean sIsEllipticProximity = (SystemProperties.getBoolean("ro.vendor.audio.us.proximity", false) || SystemProperties.getBoolean("ro.audio.us.proximity", false));
    private static boolean sIsFullScreenGestureOpened = false;
    private static boolean sIsScreenTurnOnDelayed = false;
    private static int sNeedHideChargeCircleDeviceWhenFod = -1;
    private static List<String> sRegionSupportMiHomeList = new ArrayList();
    private static List<String> sSCSlideNotOpenCameraList = new ArrayList();
    private static IWindowManager sWindowManager;
    private static int sYearOfStatistics = -1;

    public static boolean isDefaultLockScreenTheme() {
        return !new File("/data/system/theme/lockscreen").exists();
    }

    public static List<UserInfo> getUserList(Context context) {
        return ((UserManager) context.getSystemService("user")).getUsers();
    }

    public static boolean isIndianRegion(Context context) {
        return "IN".equals(KeyguardUpdateMonitor.getInstance(context).getCurrentRegion()) && Build.IS_INTERNATIONAL_BUILD;
    }

    public static boolean isFingerprintHardwareAvailable(Context context) {
        if (sFingerprintHelper == null) {
            sFingerprintHelper = new FingerprintHelper(context);
        }
        return sFingerprintHelper.isHardwareDetected();
    }

    public static boolean uriHasUserId(Uri uri) {
        if (uri == null) {
            return false;
        }
        return !TextUtils.isEmpty(uri.getUserInfo());
    }

    public static Uri maybeAddUserId(Uri uri, int userId) {
        if (Build.VERSION.SDK_INT < 21) {
            return uri;
        }
        if (uri == null) {
            return null;
        }
        if (userId == -2 || !"content".equals(uri.getScheme()) || uriHasUserId(uri)) {
            return uri;
        }
        Uri.Builder builder = uri.buildUpon();
        builder.encodedAuthority("" + userId + "@" + uri.getEncodedAuthority());
        return builder.build();
    }

    public static void setUserAuthenticatedSinceBootSecond() {
        SystemProperties.set("sys.miui.user_authenticated_sec", "true");
    }

    public static boolean hasNavigationBar() {
        if (sWindowManager == null) {
            sWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            try {
                sHasNavigationBar = sWindowManager.hasNavigationBar();
            } catch (Exception e) {
                Log.e("miui_keyguard", "no window manager to get navigation bar information");
            }
        }
        return sHasNavigationBar;
    }

    public static int getFastBlurColor(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return -1;
        }
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            while (width > 1) {
                width /= 2;
                if (width < 1) {
                    width = 1;
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            while (height > 1) {
                height /= 2;
                if (height < 1) {
                    height = 1;
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            return bitmap.getPixel(0, 0);
        } catch (Exception e) {
            Log.e("miui_keyguard", "getFastBlurColor", e);
            return -1;
        } finally {
            bitmap.recycle();
        }
    }

    public static int getFastBlurColor(Context context, Drawable drawable) {
        return getFastBlurColor(context, DrawableUtils.drawable2Bitmap(drawable));
    }

    public static int addTwoColor(int colorDown, int colorUp) {
        float alphaDown = ((float) Color.alpha(colorDown)) / 255.0f;
        float alphaUp = ((float) Color.alpha(colorUp)) / 255.0f;
        return Color.argb((int) (255.0f * ((alphaDown + alphaUp) - (alphaDown * alphaUp))), (int) ((((((float) Color.red(colorDown)) * alphaDown) * (1.0f - alphaUp)) + (((float) Color.red(colorUp)) * alphaUp)) / ((alphaDown + alphaUp) - (alphaDown * alphaUp))), (int) ((((((float) Color.green(colorDown)) * alphaDown) * (1.0f - alphaUp)) + (((float) Color.green(colorUp)) * alphaUp)) / ((alphaDown + alphaUp) - (alphaDown * alphaUp))), (int) ((((((float) Color.blue(colorDown)) * alphaDown) * (1.0f - alphaUp)) + (((float) Color.blue(colorUp)) * alphaUp)) / ((alphaDown + alphaUp) - (alphaDown * alphaUp))));
    }

    public static boolean isSupportFaceUnlock(Context context) {
        if ("ursa".equals(miui.os.Build.DEVICE) && !miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return true;
        }
        if (mFaceManager == null) {
            mFaceManager = MiuiFaceFactory.getFaceManager(context, 0);
        }
        return mFaceManager.isFaceFeatureSupport();
    }

    public static boolean isSupportPickupByMTK(Context context) {
        if (sDeviceSupportPickupByMTK.isEmpty()) {
            sDeviceSupportPickupByMTK = Arrays.asList(context.getResources().getStringArray(R.array.device_support_pickup_by_MTK));
        }
        return sDeviceSupportPickupByMTK.contains(miui.os.Build.DEVICE);
    }

    public static boolean isSupportScreenOnDelayed(Context context) {
        if (sDeviceSupportScreenOnDelay.isEmpty()) {
            sDeviceSupportScreenOnDelay = Arrays.asList(context.getResources().getStringArray(R.array.device_support_screen_on_delayed));
        }
        boolean z = true;
        if ("ursa".equals(miui.os.Build.DEVICE) && sDeviceSupportScreenOnDelay.contains(miui.os.Build.DEVICE)) {
            if (Settings.Secure.getIntForUser(context.getContentResolver(), "face_unlock_model", 0, KeyguardUpdateMonitor.getCurrentUser()) == 0) {
                z = false;
            }
            return z;
        } else if (!"perseus".equals(miui.os.Build.DEVICE) || !sDeviceSupportScreenOnDelay.contains(miui.os.Build.DEVICE)) {
            return sDeviceSupportScreenOnDelay.contains(miui.os.Build.DEVICE);
        } else {
            if (Settings.System.getIntForUser(context.getContentResolver(), "sc_status", 0, -2) != 0) {
                z = false;
            }
            return z;
        }
    }

    public static boolean isPsensorDisabled(Context context) {
        return ((SensorManager) context.getSystemService("sensor")).getDefaultSensor(8) == null || sIsEllipticProximity;
    }

    public static boolean isNonUI() {
        return SystemProperties.getBoolean("sys.power.nonui", false);
    }

    public static String formatTime(int time) {
        if (time >= 10) {
            return String.valueOf(time);
        }
        if (time <= 0) {
            return "00";
        }
        return "0" + String.valueOf(time);
    }

    public static boolean isSupportVerticalClock(int selectedClock, Context context) {
        return (selectedClock == 0 && context.getResources().getBoolean(R.bool.keyguard_show_vertical_time)) || selectedClock == 3;
    }

    public static boolean isPad() {
        return FeatureParser.getBoolean("is_pad", false);
    }

    public static boolean isGxzwSensor() {
        return GXZW_SENSOR;
    }

    public static boolean supportWirelessCharge() {
        return SUPPORT_WIRELESS_CHARGE;
    }

    public static boolean isTopActivityNeedFingerprint(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService("activity");
            List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
            if (tasks != null && tasks.size() > 0) {
                String pkg = manager.getRunningTasks(1).get(0).topActivity.getPackageName();
                if ("com.miui.tsmclient".equalsIgnoreCase(pkg) || LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME.equalsIgnoreCase(pkg)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean supportNewChargeAnimation() {
        return !FeatureParser.getBoolean("is_pad", false);
    }

    public static boolean isTopActivitySystemApp(Context context) {
        String packageName = Util.getTopActivityPkg(context);
        boolean z = false;
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            return true;
        }
        try {
            if ((context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo.flags & 1) > 0) {
                z = true;
            }
            return z;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setIsFullScreenGestureOpened(boolean isFullScreenGestureOpened) {
        sIsFullScreenGestureOpened = isFullScreenGestureOpened;
    }

    public static String getCameraImageName() {
        if (!hasNavigationBar()) {
            return "camera_preview";
        }
        if (!Constants.IS_NOTCH || !sIsFullScreenGestureOpened) {
            return "camera_preview_nvirtualkey";
        }
        return "camera_preview_notch_nvirtualkey";
    }

    public static void setScreenTurnOnDelayed(boolean isScreenTurnOnDelayed) {
        sIsScreenTurnOnDelayed = isScreenTurnOnDelayed;
    }

    public static boolean isScreenTurnOnDelayed() {
        return sIsScreenTurnOnDelayed;
    }

    public static boolean isAodClockDisable(Context context) {
        BatteryController batteryController = (BatteryController) Dependency.get(BatteryController.class);
        boolean powerSave = batteryController != null && (batteryController.isPowerSave() || batteryController.isExtremePowerSave());
        if ((Settings.Secure.getIntForUser(context.getContentResolver(), AODKeys.AOD_MODE, 0, -2) == 0) || powerSave) {
            return true;
        }
        if (!(Settings.Secure.getIntForUser(context.getContentResolver(), "aod_mode_time", 1, -2) == 1)) {
            return false;
        }
        long startTime = ((long) Settings.Secure.getIntForUser(context.getContentResolver(), "aod_start", 420, -2)) * 60000;
        long endTime = ((long) Settings.Secure.getIntForUser(context.getContentResolver(), "aod_end", 1380, -2)) * 60000;
        Calendar c = Calendar.getInstance();
        long now = (((long) ((c.get(11) * 60) + c.get(12))) * 60000) + 1;
        return (startTime < endTime && (now < startTime || now > endTime)) || (startTime > endTime && now < startTime && now > endTime);
    }

    public static boolean isAodAnimateEnable(Context context) {
        return getKeyguardNotificationStatus(context.getContentResolver()) == 2;
    }

    public static void setUserAuthenticatedSinceBoot() {
        if (!sHasSetAuth) {
            SystemProperties.set("sys.miui.user_authenticated", "true");
            sHasSetAuth = true;
        }
    }

    public static boolean isGreenKidActive(Context context) {
        return MiuiSettings.Secure.isGreenKidActive(context.getContentResolver());
    }

    public static boolean needPasswordCheck(boolean match, int userIdMatched) {
        return !match || Build.VERSION.SDK_INT < 25 || (match && userIdMatched != KeyguardUpdateMonitor.getCurrentUser());
    }

    public static boolean canSwitchUser(Context context, int userId) {
        if (userId != 0 && !KeyguardUpdateMonitor.getInstance(context).getStrongAuthTracker().hasOwnerUserAuthenticatedSinceBoot()) {
            return false;
        }
        if (isGreenKidActive(context)) {
            Log.d("MiuiKeyguardUtils", "Can't switch user when green kid active.");
            return false;
        } else if (!PhoneUtils.isInCall(context)) {
            return true;
        } else {
            Log.d("MiuiKeyguardUtils", "Can't switch user when phone calling.");
            return false;
        }
    }

    public static boolean isInvertColorsEnable(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "accessibility_display_inversion_enabled", 0, -2) != 0;
    }

    public static int getAuthUserId(Context context, int targetId) {
        int secondSpaceId = Settings.Secure.getIntForUser(context.getContentResolver(), "second_user_id", -10000, 0);
        if (secondSpaceId == -10000) {
            return 0;
        }
        HashMap<String, Integer> map = FingerprintIdUtils.getUserFingerprintIds(context, secondSpaceId);
        if (map == null || map.size() == 0 || !map.containsKey(String.valueOf(targetId))) {
            return 0;
        }
        return map.get(String.valueOf(targetId)).intValue();
    }

    public static void recordKeyguardSettingsStatistics(Context context) {
        MiuiLockPatternUtils lockPatternUtils = new MiuiLockPatternUtils(context);
        boolean showLunarCalendarInfoEnabled = false;
        boolean isSecure = lockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || KeyguardUpdateMonitor.getInstance(context).isSimPinSecure();
        if (!isSecure) {
            AnalyticsHelper.recordUnlockWay("none");
        }
        Calendar calendar = Calendar.getInstance();
        if (sYearOfStatistics != calendar.get(1) || sDayOfStatistics != calendar.get(6)) {
            sYearOfStatistics = calendar.get(1);
            sDayOfStatistics = calendar.get(6);
            LockPatternUtilsWrapper lockPatternUtilsWrapper = new LockPatternUtilsWrapper(lockPatternUtils);
            String secureType = "unsecure";
            if (isSecure) {
                int activePasswordQuality = lockPatternUtilsWrapper.getActivePasswordQuality();
                if (activePasswordQuality == 65536) {
                    secureType = "pattern";
                } else if (activePasswordQuality == 131072 || activePasswordQuality == 196608) {
                    secureType = "numeric";
                } else if (activePasswordQuality == 262144 || activePasswordQuality == 327680 || activePasswordQuality == 393216) {
                    secureType = "mixed";
                }
            }
            AnalyticsHelper.recordEnum("keyguard_secure_type", secureType);
            if (isSupportFaceUnlock(context)) {
                String state = "unsecure";
                if (KeyguardUpdateMonitor.getInstance(context).hasFaceUnlockData() && KeyguardUpdateMonitor.getInstance(context).faceUnlockApplyLock()) {
                    state = "enable";
                } else if (KeyguardUpdateMonitor.getInstance(context).hasFaceUnlockData()) {
                    state = "disable";
                }
                AnalyticsHelper.recordEnum("face_unlock_state", state);
                AnalyticsHelper.recordEnum("face_unlock_notification_toggle", String.valueOf(Settings.Secure.getIntForUser(context.getContentResolver(), "face_unlock_by_notification_screen_on", 0, KeyguardUpdateMonitor.getCurrentUser())));
            }
            AnalyticsHelper.recordEnum("keyguard_screen_on_when_notification", Boolean.toString(isWakeupForNotification(context.getContentResolver())));
            AnalyticsHelper.recordEnum("keyguard_has_owner_info", Boolean.toString(new LockPatternUtilsWrapper(lockPatternUtils).isOwnerInfoEnabled() && !TextUtils.isEmpty(lockPatternUtils.getOwnerInfo(UserHandle.myUserId()))));
            if (Settings.System.getInt(context.getContentResolver(), "show_lunar_calendar", 0) == 1) {
                showLunarCalendarInfoEnabled = true;
            }
            AnalyticsHelper.recordEnum("keyguard_show_lunar_calendar", Boolean.toString(showLunarCalendarInfoEnabled));
            AnalyticsHelper.recordEnum("keyguard_auto_lock", Long.toString(Settings.System.getLongForUser(context.getContentResolver(), "screen_off_timeout", 30000, KeyguardUpdateMonitor.getCurrentUser())));
            if (isFingerprintHardwareAvailable(context)) {
                String state2 = "not_secure";
                if (isSecure) {
                    MiuiLockPatternUtils miuiLockPatternUtils = lockPatternUtils;
                    if (Settings.Secure.getIntForUser(context.getContentResolver(), "miui_keyguard", 2, KeyguardUpdateMonitor.getCurrentUser()) == 2) {
                        state2 = "enabled";
                    } else {
                        state2 = "disabled";
                    }
                }
                AnalyticsHelper.recordEnum("keyguard_fingerprint_state_new", state2);
            }
            LockScreenMagazineAnalytics.recordLockScreenWallperProviderStatus();
        }
    }

    public static boolean isSCSlideNotOpenCamera(Context context) {
        if (sSCSlideNotOpenCameraList.isEmpty()) {
            sSCSlideNotOpenCameraList = Arrays.asList(context.getResources().getStringArray(R.array.lockscreen_sc_slide_not_open_camera));
        }
        String packageName = Util.getTopActivityPkg(context);
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return sSCSlideNotOpenCameraList.contains(context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo.packageName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isDozing() {
        return Dependency.getHost() != null && Dependency.getHost().isDozing();
    }

    public static boolean isSupportAodAnimateDevice() {
        return "perseus".equals(android.os.Build.DEVICE);
    }

    public static int getKeyguardNotificationStatus(ContentResolver contentResolver) {
        int i;
        if (isSupportAodAnimateDevice()) {
            i = 2;
        } else {
            i = 1;
        }
        return Settings.System.getIntForUser(contentResolver, "wakeup_for_keyguard_notification", i, KeyguardUpdateMonitor.getCurrentUser());
    }

    public static boolean isWakeupForNotification(ContentResolver contentResolver) {
        return getKeyguardNotificationStatus(contentResolver) == 1;
    }

    public static boolean showMXTelcelLockScreen(Context context) {
        return "mx_telcel".equals(CUSTOMIZED_REGION) && isAppRunning(context, "com.celltick.lockscreen");
    }

    public static boolean isAppRunning(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> infoList = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
        if (infoList == null || infoList.size() == 0) {
            return false;
        }
        for (int i = 0; i < infoList.size(); i++) {
            ActivityManager.RunningAppProcessInfo runningApp = infoList.get(i);
            if (packageName.equals(runningApp.processName)) {
                return true;
            }
            String[] pkgList = runningApp.pkgList;
            if (pkgList != null) {
                for (String equals : pkgList) {
                    if (packageName.equals(equals)) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    public static boolean canShowChargeCircle(Context context) {
        return !isGxzwSensor() || !isNeedHideChargeCircleDeviceWhenFod(context) || !KeyguardUpdateMonitor.getInstance(context).isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser());
    }

    private static boolean isNeedHideChargeCircleDeviceWhenFod(Context context) {
        if (sNeedHideChargeCircleDeviceWhenFod == -1) {
            sNeedHideChargeCircleDeviceWhenFod = Arrays.asList(context.getResources().getStringArray(R.array.devices_hide_charge_circle_when_fod)).contains(miui.os.Build.DEVICE) ? 1 : 0;
        }
        return sNeedHideChargeCircleDeviceWhenFod == 1;
    }

    public static boolean supportDualClock() {
        return "perseus".equals(miui.os.Build.DEVICE);
    }

    public static void setViewTouchDelegate(final View view, final int expandTouchWidth) {
        if (view != null) {
            final View parentView = (View) view.getParent();
            parentView.post(new Runnable() {
                public void run() {
                    TouchDelegate touchDelegate = null;
                    if (expandTouchWidth != 0) {
                        Rect rect = new Rect();
                        view.getHitRect(rect);
                        rect.top -= expandTouchWidth;
                        rect.bottom += expandTouchWidth;
                        rect.left -= expandTouchWidth;
                        rect.right += expandTouchWidth;
                        touchDelegate = new TouchDelegate(rect, view);
                    }
                    parentView.setTouchDelegate(touchDelegate);
                }
            });
        }
    }

    public static boolean isRegionSupportMiHome(Context context) {
        if (sRegionSupportMiHomeList.isEmpty()) {
            sRegionSupportMiHomeList = Arrays.asList(context.getResources().getStringArray(R.array.region_support_mihome));
        }
        return sRegionSupportMiHomeList.contains(miui.os.Build.getRegion());
    }

    public static int getBitmapColorMode(Bitmap bmp, int sampleRatio) {
        try {
            return BitmapFactory.getBitmapColorMode(bmp, sampleRatio);
        } catch (Exception e) {
            Log.e("MiuiKeyguardUtils", "getBitmapColorMode", e);
            return 2;
        }
    }

    public static boolean isSupportLiftingCamera(Context context) {
        if (sDeviceSupportLiftingCamera.isEmpty()) {
            sDeviceSupportLiftingCamera = Arrays.asList(context.getResources().getStringArray(R.array.device_support_lifting_camera));
        }
        return sDeviceSupportLiftingCamera.contains(miui.os.Build.DEVICE);
    }
}
