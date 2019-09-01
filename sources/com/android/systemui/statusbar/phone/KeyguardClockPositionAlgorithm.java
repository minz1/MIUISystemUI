package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.graphics.Path;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.NotificationUtils;

public class KeyguardClockPositionAlgorithm {
    private static final PathInterpolator sSlowDownInterpolator;
    private AccelerateInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    private int mClockBottom;
    private float mClockMarginTop;
    private int mClockNotificationsMarginMax;
    private int mClockNotificationsMarginMin;
    private float mClockNotificationsPadding;
    private float mClockYFractionMax;
    private float mClockYFractionMin;
    private float mDarkAmount;
    private float mDensity;
    private float mEmptyDragAmount;
    private float mExpandedHeight;
    private int mHeight;
    private int mKeyguardStatusHeight;
    private float mKeyguardVisibleViewsHeight;
    private int mMaxKeyguardNotifications;
    private int mMaxPanelHeight;
    private float mMoreCardNotificationAmount;
    private int mNotificationCount;

    public static class Result {
        public float clockAlpha;
        public float clockScale;
        public int clockY;
        public int stackScrollerPadding;
        public int stackScrollerPaddingAdjustment;
    }

    static {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(0.3f, 0.875f, 0.6f, 1.0f, 1.0f, 1.0f);
        sSlowDownInterpolator = new PathInterpolator(path);
    }

    public void loadDimens(Resources res) {
        this.mClockNotificationsMarginMin = res.getDimensionPixelSize(R.dimen.keyguard_clock_notifications_margin_min);
        this.mClockNotificationsMarginMax = res.getDimensionPixelSize(R.dimen.keyguard_clock_notifications_margin_max);
        this.mClockYFractionMin = res.getFraction(R.fraction.keyguard_clock_y_fraction_min, 1, 1);
        this.mClockYFractionMax = res.getFraction(R.fraction.keyguard_clock_y_fraction_max, 1, 1);
        this.mMoreCardNotificationAmount = ((float) res.getDimensionPixelSize(R.dimen.notification_shelf_height)) / ((float) res.getDimensionPixelSize(R.dimen.notification_min_height));
        this.mDensity = res.getDisplayMetrics().density;
        this.mClockMarginTop = (float) res.getDimensionPixelSize(R.dimen.miui_keyguard_clock_magin_top);
        this.mClockNotificationsPadding = (float) res.getDimensionPixelSize(R.dimen.miui_keyguard_clock_stack_scroller_padding_top);
    }

    public void setup(int maxKeyguardNotifications, int maxPanelHeight, float expandedHeight, int notificationCount, int height, int keyguardStatusHeight, float emptyDragAmount, int clockBottom, float dark, float keyguardVisibleViewsHeight, float topMargin) {
        this.mMaxKeyguardNotifications = maxKeyguardNotifications;
        this.mMaxPanelHeight = maxPanelHeight;
        this.mExpandedHeight = expandedHeight;
        this.mNotificationCount = notificationCount;
        this.mHeight = height;
        this.mKeyguardStatusHeight = keyguardStatusHeight;
        this.mKeyguardVisibleViewsHeight = keyguardVisibleViewsHeight;
        this.mEmptyDragAmount = emptyDragAmount;
        this.mClockBottom = clockBottom;
        this.mDarkAmount = dark;
        this.mClockMarginTop = topMargin;
    }

    public void run(Result result) {
        int y = getClockY() - (this.mKeyguardStatusHeight / 2);
        result.stackScrollerPaddingAdjustment = 0;
        result.clockY = 0;
        result.stackScrollerPadding = (int) (this.mKeyguardVisibleViewsHeight + ((float) ((int) (this.mClockMarginTop + this.mClockNotificationsPadding))));
        result.clockScale = 1.0f;
        result.clockAlpha = 1.0f;
        result.stackScrollerPadding = (int) NotificationUtils.interpolate((float) result.stackScrollerPadding, (float) (this.mClockBottom + y), this.mDarkAmount);
    }

    private float getClockYFraction() {
        float t = Math.min(getNotificationAmountT(), 1.0f);
        return ((1.0f - t) * this.mClockYFractionMax) + (this.mClockYFractionMin * t);
    }

    private int getClockY() {
        return (int) NotificationUtils.interpolate(getClockYFraction() * ((float) this.mHeight), ((0.33f * ((float) this.mHeight)) + (((float) this.mKeyguardStatusHeight) / 2.0f)) - ((float) this.mClockBottom), this.mDarkAmount);
    }

    private float getNotificationAmountT() {
        return ((float) this.mNotificationCount) / (((float) this.mMaxKeyguardNotifications) + this.mMoreCardNotificationAmount);
    }

    public String toString() {
        return "{mHeight=" + this.mHeight + ", mKeyguardStatusHeight=" + this.mKeyguardStatusHeight + ", mKeyguardVisibleViewsHeight=" + this.mKeyguardVisibleViewsHeight + ", mClockMarginTop=" + this.mClockMarginTop + ", mClockNotificationsPadding=" + this.mClockNotificationsPadding + ", mClockBottom=" + this.mClockBottom + ", clockY=" + getClockY() + ", mDarkAmount=" + this.mDarkAmount + '}';
    }
}
