package com.android.systemui.miui.statusbar.notification;

import android.app.ActivityThread;
import android.app.Notification;
import android.app.NotificationCompat;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.AppIconsManager;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.policy.UsbNotificationController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.NotificationViewWrapperCompat;
import miui.content.res.IconCustomizer;
import miui.securityspace.CrossUserUtils;
import miui.securityspace.XSpaceUserHandle;
import miui.util.NotificationFilterHelper;

public class NotificationUtil {
    private static boolean sFold = false;
    private static boolean sFoldAnimating = false;
    private static boolean sIsLastQsCovered = false;
    private static int sNotificationStyle;
    private static boolean sUserFold = false;

    static {
        int i = 0;
        if (Constants.SHOW_NOTIFICATION_HEADER) {
            i = 1;
        }
        sNotificationStyle = i;
    }

    public static boolean isFold() {
        return sFold;
    }

    public static boolean isUserFold() {
        return sUserFold;
    }

    public static void fold(boolean fold) {
        sFold = fold;
    }

    public static void userFold(int userFold) {
        if (userFold > 0) {
            sUserFold = true;
        } else if (userFold < 0) {
            sUserFold = false;
        } else {
            sUserFold = FoldBucketHelper.allowFold();
        }
    }

    public static void setFoldAnimating(boolean foldAnimating) {
        sFoldAnimating = foldAnimating;
    }

    public static boolean isFoldAnimating() {
        return sFoldAnimating;
    }

    public static boolean isLastQsCovered() {
        return sIsLastQsCovered;
    }

    public static void lastQsCovered(boolean qsCovered) {
        sIsLastQsCovered = qsCovered;
    }

    public static float getFoldTranslationDirection(boolean add, float origin) {
        if (!isFoldAnimating()) {
            return origin;
        }
        return ((float) (isFold() ^ add ? -1 : 1)) * 1.0f;
    }

    public static int getUserFoldLinesCount() {
        return 0;
    }

    public static boolean isXmsf(ExpandedNotification sbn) {
        return sbn != null && "com.xiaomi.xmsf".equals(sbn.getBasePkg());
    }

    public static boolean isHybrid(ExpandedNotification sbn) {
        return sbn != null && "com.miui.hybrid".equals(sbn.getBasePkg());
    }

    public static boolean isCts(ExpandedNotification sbn) {
        return sbn != null && "com.android.cts.verifier".equals(sbn.getBasePkg());
    }

    public static boolean isXmsfCategory(ExpandedNotification sbn) {
        return "com.xiaomi.xmsf".equals(sbn.getPackageName()) && !TextUtils.isEmpty(getCategory(sbn));
    }

    public static String getHybridAppName(ExpandedNotification sbn) {
        if (sbn.getNotification() == null || sbn.getNotification().extras == null) {
            return null;
        }
        return sbn.getNotification().extras.getString("miui.substName");
    }

    public static String getMessageId(ExpandedNotification sbn) {
        if (sbn.getNotification() == null || sbn.getNotification().extras == null) {
            return null;
        }
        String messageId = sbn.getNotification().extras.getString("message_id");
        if (TextUtils.isEmpty(messageId)) {
            long ad_id = sbn.getNotification().extras.getLong("adid");
            if (ad_id != 0) {
                messageId = String.valueOf(ad_id);
            }
        }
        return messageId;
    }

    public static String getCategory(ExpandedNotification sbn) {
        if (sbn.getNotification() == null || sbn.getNotification().extras == null) {
            return null;
        }
        return sbn.getNotification().extras.getString("miui.category");
    }

    public static void setPkgImportance(Context context, String pkg, int importance) {
        NotificationFilterHelper.setImportance(context, pkg, importance);
    }

    public static int getPkgImportance(Context context, String pkg) {
        return NotificationFilterHelper.getImportance(context, pkg);
    }

    public static boolean canSendNotificationForTargetPkg(ExpandedNotification notification) {
        return "com.xiaomi.xmsf".equals(notification.getBasePkg()) || "com.android.systemui".equals(notification.getBasePkg()) || "com.android.keyguard".equals(notification.getBasePkg()) || "com.android.phone".equals(notification.getBasePkg()) || "com.miui.systemAdSolution".equals(notification.getBasePkg()) || "com.miui.msa.global".equals(notification.getBasePkg()) || Constants.DEBUG;
    }

    public static boolean isPkgWontAutoBundle(String pkgName) {
        return "com.xiaomi.xmsf".equals(pkgName) || "com.miui.hybrid".equals(pkgName) || "com.miui.systemAdSolution".equals(pkgName) || "com.android.systemui".equals(pkgName);
    }

    public static boolean isPkgInFoldWhiteList(String pkgName) {
        return "com.miui.securitycenter".equals(pkgName) || "com.lbe.security.miui".equals(pkgName);
    }

    public static boolean hasSmallIcon(Notification n) {
        return n.getSmallIcon() != null;
    }

    private static boolean hasLargeIcon(Notification n) {
        return (n.largeIcon == null && n.getLargeIcon() == null) ? false : true;
    }

    public static Drawable getLargeIconDrawable(Context context, Notification n) {
        if (n.largeIcon != null) {
            return new BitmapDrawable(context.getResources(), n.largeIcon);
        }
        if (n.getLargeIcon() != null) {
            return n.getLargeIcon().loadDrawable(context);
        }
        return null;
    }

    public static void applyLegacyRowIcon(Context context, ExpandedNotification sbn, View expandedView) {
        Drawable iconDrawable = sbn.getRowIcon() != null ? sbn.getRowIcon() : sbn.getAppIcon();
        if (isCustomViewNotification(sbn) && !NotificationViewWrapperCompat.isNotificationHeader(expandedView)) {
            ImageView icon = (ImageView) expandedView.findViewById(16908294);
            if (icon != null) {
                if (iconDrawable != null) {
                    icon.setImageDrawable(XSpaceUserHandle.getXSpaceIcon(context, iconDrawable, sbn.getUser()));
                }
                icon.setBackground(null);
                ViewGroup.LayoutParams lp = icon.getLayoutParams();
                lp.width = context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width);
                lp.height = context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_height);
                icon.setLayoutParams(lp);
                icon.setTag(R.id.custom_view_icon_applied, true);
            }
        }
    }

    public static void applyAppIcon(Context context, ExpandedNotification sbn, ImageView rowIcon) {
        if (rowIcon != null) {
            Drawable iconDrawable = sbn.getAppIcon();
            if (isHybrid(sbn) && hasLargeIcon(sbn.getNotification())) {
                iconDrawable = getLargeIconDrawable(context, sbn.getNotification());
            }
            if (iconDrawable == null) {
                iconDrawable = sbn.getNotification().getSmallIcon().loadDrawable(context);
            }
            if (iconDrawable != null) {
                rowIcon.setImageDrawable(XSpaceUserHandle.getXSpaceIcon(context, iconDrawable, sbn.getUser()));
            }
        }
    }

    public static void applyRightIcon(Context context, ExpandedNotification sbn, ImageView rightIcon) {
        Drawable iconDrawable;
        if (rightIcon != null) {
            if (hasLargeIcon(sbn.getNotification())) {
                iconDrawable = getLargeIconDrawable(context, sbn.getNotification());
                if (iconDrawable != null && !isMediaNotification(sbn)) {
                    iconDrawable = ((AppIconsManager) Dependency.get(AppIconsManager.class)).getIconStyleDrawable(iconDrawable, false);
                }
            } else {
                iconDrawable = sbn.getRowIcon();
                if (iconDrawable == null) {
                    iconDrawable = sbn.getAppIcon();
                }
                if (iconDrawable == null) {
                    iconDrawable = sbn.getNotification().getSmallIcon().loadDrawable(context);
                }
            }
            if (iconDrawable != null) {
                rightIcon.setImageDrawable(XSpaceUserHandle.getXSpaceIcon(context, iconDrawable, sbn.getUser()));
            }
        }
    }

    public static Drawable getRowIcon(Context context, ExpandedNotification en) {
        if (isPhoneNotification(en)) {
            return IconCustomizer.getCustomizedIcon(context, "com.android.contacts.activities.TwelveKeyDialer.png");
        }
        if (((UsbNotificationController) Dependency.get(UsbNotificationController.class)).isUsbNotification(en)) {
            Drawable drawable = context.getResources().getDrawable(R.drawable.notification_usb);
            if (drawable != null) {
                return ((AppIconsManager) Dependency.get(AppIconsManager.class)).getIconStyleDrawable(drawable, true);
            }
        } else if (isImeNotification(en)) {
            Drawable drawable2 = context.getResources().getDrawable(R.drawable.notification_ime);
            if (drawable2 != null) {
                return ((AppIconsManager) Dependency.get(AppIconsManager.class)).getIconStyleDrawable(drawable2, true);
            }
        } else if (en.isSubstituteNotification()) {
            int userId = en.getUser().getIdentifier();
            if (userId < 0) {
                userId = CrossUserUtils.getCurrentUserId();
            }
            return ((AppIconsManager) Dependency.get(AppIconsManager.class)).getAppIcon(context, en.getPackageName(), userId);
        }
        return null;
    }

    public static boolean isSystemNotification(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        return "android".equals(pkg) || "com.android.systemui".equals(pkg);
    }

    public static boolean isInCallUINotification(ExpandedNotification notification) {
        return "com.android.incallui".equals(notification.getPackageName());
    }

    private static boolean isPhoneNotification(ExpandedNotification notification) {
        String pkg = notification.getPackageName();
        return "com.android.incallui".equals(pkg) || "com.android.phone".equals(pkg) || "com.android.server.telecom".equals(pkg);
    }

    private static boolean isImeNotification(ExpandedNotification notification) {
        int id = notification.getId();
        return "android".equals(notification.getPackageName()) && (id == 17040922 || id == 8);
    }

    public static boolean isMissedCallNotification(ExpandedNotification notification) {
        boolean z = false;
        if (notification == null) {
            return false;
        }
        String basePkg = notification.getBasePkg();
        if (("com.android.phone".equals(basePkg) || "com.android.server.telecom".equals(basePkg) || "com.miui.voip".equals(basePkg)) && "missed_call".equals(notification.getTag())) {
            z = true;
        }
        return z;
    }

    public static CharSequence getMessageClassName(ExpandedNotification notification) {
        CharSequence className = notification.getNotification().extraNotification.getMessageClassName();
        return className == null ? "" : className;
    }

    public static boolean needStatBadgeNum(NotificationData.Entry entry) {
        return entry != null && needStatBadgeNum(entry.notification);
    }

    public static boolean needStatBadgeNum(ExpandedNotification notification) {
        return notification != null && !"com.android.systemui".equals(notification.getPackageName()) && !hasProgressbar(notification) && notification.isClearable() && !notification.isFold();
    }

    public static boolean needRestatBadgeNum(ExpandedNotification newNotification, ExpandedNotification oldNotification) {
        return (newNotification.getNotification().extraNotification.getMessageCount() == oldNotification.getNotification().extraNotification.getMessageCount() && needStatBadgeNum(newNotification) == needStatBadgeNum(oldNotification)) ? false : true;
    }

    public static boolean hasProgressbar(ExpandedNotification notification) {
        boolean z = false;
        if (notification == null) {
            return false;
        }
        Bundle extras = notification.getNotification().extras;
        int max = extras.getInt("android.progressMax", 0);
        boolean ind = extras.getBoolean("android.progressIndeterminate");
        if (max != 0 || ind) {
            z = true;
        }
        return z;
    }

    public static Icon getSmallIcon(Context context, ExpandedNotification sbn) {
        if (shouldSubstituteSmallIcon(sbn)) {
            int userId = sbn.getUser().getIdentifier();
            if (userId < 0) {
                userId = CrossUserUtils.getCurrentUserId();
            }
            Bitmap bitmap = ((AppIconsManager) Dependency.get(AppIconsManager.class)).getAppIconBitmap(context, sbn.getPackageName(), userId);
            if (bitmap != null) {
                return Icon.createWithBitmap(bitmap);
            }
        }
        return sbn.getNotification().getSmallIcon();
    }

    public static boolean isGrayscaleIcon(Notification notification) {
        return notification.extras.getBoolean("miui.isGrayscaleIcon", false);
    }

    public static boolean shouldSubstituteSmallIcon(ExpandedNotification sbn) {
        return sbn.isSubstituteNotification() && !isGrayscaleIcon(sbn.getNotification());
    }

    public static boolean isMediaNotification(ExpandedNotification notification) {
        return notification != null && NotificationCompat.isMediaNotification(notification.getNotification());
    }

    public static boolean isCustomViewNotification(ExpandedNotification notification) {
        return (notification == null || (notification.getNotification().contentView == null && notification.getNotification().bigContentView == null)) ? false : true;
    }

    public static Context getPackageContext(Context context, StatusBarNotification sbn) {
        Context packageContext = sbn.getPackageContext(context);
        packageContext.setTheme(miui.R.style.Theme_Light_RemoteViews);
        return packageContext;
    }

    public static boolean isExpandingEnabled(boolean onKeyguard) {
        return Constants.SHOW_NOTIFICATION_HEADER && !onKeyguard;
    }

    public static int getNotificationStyle() {
        return sNotificationStyle;
    }

    public static boolean showMiuiStyle() {
        return sNotificationStyle == 0;
    }

    public static boolean showGoogleStyle() {
        return sNotificationStyle == 1;
    }

    public static boolean isNotificationStyleChanged(int style) {
        boolean changed = sNotificationStyle != style;
        sNotificationStyle = style;
        return changed;
    }

    public static int getUid(ExpandedNotification notification) {
        if (canSendNotificationForTargetPkg(notification)) {
            try {
                ApplicationInfo info = ActivityThread.getPackageManager().getApplicationInfo(notification.getPackageName(), 0, notification.getUser().getIdentifier());
                if (info != null) {
                    return info.uid;
                }
            } catch (Exception e) {
                Log.e("NotificationUtil", "error getting uid", e);
            }
        }
        return notification.getUid();
    }

    public static String getHiddenText(Context context) {
        int i;
        if (Build.VERSION.SDK_INT >= 27) {
            i = R.string.notification_hidden_text;
        } else {
            i = R.string.notification_hidden_by_policy_text;
        }
        return context.getString(i);
    }

    public static CharSequence resolveTitle(Notification notification) {
        CharSequence title = notification.extras.getCharSequence("android.title");
        if (title == null) {
            title = notification.extras.getCharSequence("android.title.big");
        }
        return title != null ? title : "";
    }

    public static CharSequence resolveText(Notification notification) {
        CharSequence content = notification.extras.getCharSequence("android.text");
        if (content == null) {
            content = notification.extras.getCharSequence("android.bigText");
        }
        return content != null ? content : "";
    }

    public static CharSequence resolveSubText(Notification notification) {
        CharSequence subText = notification.extras.getCharSequence("android.subText");
        return subText != null ? subText : "";
    }
}
