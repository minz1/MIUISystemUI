package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.keyguard.Ease;
import com.android.systemui.Constants;
import com.android.systemui.R;

public class MiuiKeyguardVerticalClock extends MiuiKeyguardBaseClock {
    /* access modifiers changed from: private */
    public int mHeight;
    protected TextView mHorizontalDot;
    protected TextView mHorizontalHour;
    protected TextView mHorizontalMin;
    protected LinearLayout mHorizontalTimeLayout;
    /* access modifiers changed from: private */
    public float mHorizontalTimeLayoutHeight;
    private float mHorizontalTimePaddingTop;
    private AnimatorSet mHorizontalToVerticalAnim;
    View.OnLayoutChangeListener mLayoutChangeListener;
    private PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public boolean mShowHorizontalTime;
    private boolean mSkipAnimation;
    private FrameLayout mTimeLayout;
    private TextView mVerticalHour;
    private TextView mVerticalMin;
    /* access modifiers changed from: private */
    public LinearLayout mVerticalTimeLayout;
    /* access modifiers changed from: private */
    public float mVerticalTimeLayoutHeight;
    private float mVerticalTimePaddingTop;
    private AnimatorSet mVerticalToHorizontalAnim;

    public MiuiKeyguardVerticalClock(Context context) {
        this(context, null);
    }

    public MiuiKeyguardVerticalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowHorizontalTime = false;
        this.mSkipAnimation = false;
        this.mHorizontalToVerticalAnim = new AnimatorSet();
        this.mVerticalToHorizontalAnim = new AnimatorSet();
        this.mLayoutChangeListener = new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (MiuiKeyguardVerticalClock.this.getHeight() > 0) {
                    int unused = MiuiKeyguardVerticalClock.this.mHeight = MiuiKeyguardVerticalClock.this.getHeight();
                }
            }
        };
        this.mPowerManager = (PowerManager) context.getSystemService("power");
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mHorizontalHour = (TextView) findViewById(R.id.horizontal_hour);
        this.mHorizontalDot = (TextView) findViewById(R.id.horizontal_dot);
        this.mHorizontalMin = (TextView) findViewById(R.id.horizontal_min);
        this.mTimeLayout = (FrameLayout) findViewById(R.id.time_layout);
        this.mVerticalHour = (TextView) findViewById(R.id.vertical_hour);
        this.mVerticalMin = (TextView) findViewById(R.id.vertical_min);
        this.mVerticalTimeLayout = (LinearLayout) findViewById(R.id.vertical_time_layout);
        this.mHorizontalTimeLayout = (LinearLayout) findViewById(R.id.horizontal_time_layout);
        this.mVerticalHour.setTypeface(this.mThinFontTypeface);
        this.mVerticalMin.setTypeface(this.mThinFontTypeface);
        this.mHorizontalTimePaddingTop = (float) getResources().getDimensionPixelSize(R.dimen.keyguard_horizontal_time_margin_top);
        this.mVerticalTimePaddingTop = (float) getResources().getDimensionPixelSize(R.dimen.keyguard_vertical_time_margin_top);
        this.mVerticalTimeLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                if (height > 0) {
                    float unused = MiuiKeyguardVerticalClock.this.mVerticalTimeLayoutHeight = (float) height;
                    MiuiKeyguardVerticalClock.this.updateClockView();
                    MiuiKeyguardVerticalClock.this.mVerticalTimeLayout.removeOnLayoutChangeListener(this);
                }
            }
        });
        this.mHorizontalTimeLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                if (height > 0) {
                    float unused = MiuiKeyguardVerticalClock.this.mHorizontalTimeLayoutHeight = (float) height;
                    MiuiKeyguardVerticalClock.this.updateClockView();
                    MiuiKeyguardVerticalClock.this.mHorizontalTimeLayout.removeOnLayoutChangeListener(this);
                }
            }
        });
        this.mHorizontalTimeLayout.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(this.mContext));
        this.mVerticalTimeLayout.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(this.mContext));
        updateViewsForNotch();
    }

    /* access modifiers changed from: protected */
    public void updateViewsForNotch() {
        int i;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.mTimeLayout.getLayoutParams();
        Resources resources = this.mContext.getResources();
        if (Constants.IS_NOTCH) {
            i = R.dimen.keyguard_horizontal_time_notch_margin_top;
        } else {
            i = R.dimen.keyguard_horizontal_time_margin_top;
        }
        lp.topMargin = resources.getDimensionPixelSize(i);
        this.mTimeLayout.setLayoutParams(lp);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(this.mLayoutChangeListener);
    }

    public void setDarkMode(boolean darkMode) {
        super.setDarkMode(darkMode);
        this.mDarkMode = darkMode;
        int color = darkMode ? getContext().getResources().getColor(R.color.miui_common_unlock_screen_common_time_dark_text_color) : -1;
        this.mVerticalHour.setTextColor(color);
        this.mVerticalMin.setTextColor(color);
        this.mHorizontalHour.setTextColor(color);
        this.mHorizontalDot.setTextColor(color);
        this.mHorizontalMin.setTextColor(color);
        updateTime();
        updateDrawableResources();
    }

    public int getClockHeight() {
        return this.mHeight > 0 ? this.mHeight : getMeasuredHeight();
    }

    public float getTopMargin() {
        return (float) ((LinearLayout.LayoutParams) this.mTimeLayout.getLayoutParams()).topMargin;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(this.mLayoutChangeListener);
        clearAnim();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mFontScaleChanged) {
            int verticalTimeLayoutHeight = this.mVerticalTimeLayout.getMeasuredHeight();
            int horizontalTimeLayoutHeight = this.mHorizontalTimeLayout.getMeasuredHeight();
            if ((verticalTimeLayoutHeight > 0 && ((float) verticalTimeLayoutHeight) != this.mVerticalTimeLayoutHeight) || (horizontalTimeLayoutHeight > 0 && ((float) horizontalTimeLayoutHeight) != this.mHorizontalTimeLayoutHeight)) {
                this.mVerticalTimeLayoutHeight = (float) this.mVerticalTimeLayout.getMeasuredHeight();
                this.mHorizontalTimeLayoutHeight = (float) this.mHorizontalTimeLayout.getMeasuredHeight();
                updateClockView();
                this.mFontScaleChanged = false;
            }
            if (getMeasuredHeight() > 0) {
                this.mHeight = getMeasuredHeight();
            }
        }
    }

    public void setSkipAnimation(boolean skipAnimation) {
        this.mSkipAnimation = skipAnimation;
    }

    public void updateTime() {
        super.updateTime();
        int hour = this.mCalendar.get(18);
        int i = 12;
        int hour2 = (this.m24HourFormat || hour <= 12) ? hour : hour - 12;
        if (this.m24HourFormat || hour2 != 0) {
            i = hour2;
        }
        int hour3 = i;
        int minute = this.mCalendar.get(20);
        this.mVerticalHour.setText(MiuiKeyguardUtils.formatTime(hour3));
        this.mVerticalMin.setText(MiuiKeyguardUtils.formatTime(minute));
        this.mHorizontalHour.setText(String.valueOf(hour3));
        this.mHorizontalMin.setText(MiuiKeyguardUtils.formatTime(minute));
        LinearLayout linearLayout = this.mHorizontalTimeLayout;
        linearLayout.setContentDescription(String.valueOf(hour3) + ":" + MiuiKeyguardUtils.formatTime(minute));
        LinearLayout linearLayout2 = this.mVerticalTimeLayout;
        linearLayout2.setContentDescription(String.valueOf(hour3) + ":" + MiuiKeyguardUtils.formatTime(minute));
    }

    /* access modifiers changed from: protected */
    public void updateViewsTextSize() {
        Resources resources = this.mContext.getResources();
        int horizontalTextSize = resources.getDimensionPixelSize(R.dimen.keyguard_horizontal_time_text_size);
        int verticalTextSize = resources.getDimensionPixelSize(R.dimen.keyguard_vertical_time_text_size);
        int dateBatteryTextSize = resources.getDimensionPixelSize(R.dimen.miui_common_unlock_screen_date_text_size);
        int ownerSimTextSize = resources.getDimensionPixelSize(R.dimen.miui_common_unlock_screen_onwer_info_text_size);
        this.mHorizontalHour.setTextSize(0, (float) horizontalTextSize);
        this.mVerticalHour.setTextSize(0, (float) verticalTextSize);
        this.mHorizontalMin.setTextSize(0, (float) horizontalTextSize);
        this.mVerticalMin.setTextSize(0, (float) verticalTextSize);
        this.mHorizontalDot.setTextSize(0, (float) horizontalTextSize);
        this.mCurrentDate.setTextSize(0, (float) dateBatteryTextSize);
        this.mBatteryInfo.setTextSize(0, (float) dateBatteryTextSize);
        this.mLunarCalendarInfo.setTextSize(0, (float) ownerSimTextSize);
        this.mOwnerInfo.setTextSize(0, (float) ownerSimTextSize);
        this.mLockScreenMagazineInfo.setTextSize(0, (float) ownerSimTextSize);
        updateOwnerInfoHeight();
        updateLunarCalendarInfoHeight();
    }

    /* access modifiers changed from: protected */
    public void updateViewsLayoutParams() {
        FrameLayout.LayoutParams clockLayoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        clockLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.miui_keyguard_clock_magin_top);
        setLayoutParams(clockLayoutParams);
        updateViewsForNotch();
        this.mVerticalHour.setPadding(0, this.mResources.getDimensionPixelSize(R.dimen.keyguard_vertical_time_margin_top), 0, 0);
        LinearLayout.LayoutParams verticalMinLayoutParams = (LinearLayout.LayoutParams) this.mVerticalMin.getLayoutParams();
        verticalMinLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_vertical_time_min_top_margin_hour);
        this.mVerticalMin.setLayoutParams(verticalMinLayoutParams);
        this.mHorizontalTimeLayout.setPadding(0, this.mResources.getDimensionPixelOffset(R.dimen.keyguard_horizontal_time_margin_top), 0, 0);
        LinearLayout.LayoutParams dateInfoLayoutParams = (LinearLayout.LayoutParams) this.mDateAndBatteryInfoLayout.getLayoutParams();
        dateInfoLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_date_info_top_margin);
        this.mDateAndBatteryInfoLayout.setLayoutParams(dateInfoLayoutParams);
        LinearLayout.LayoutParams lunarCalendarInfoLayoutParams = (LinearLayout.LayoutParams) this.mLunarCalendarInfo.getLayoutParams();
        lunarCalendarInfoLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_owner_info_top_margin);
        this.mLunarCalendarInfo.setLayoutParams(lunarCalendarInfoLayoutParams);
        LinearLayout.LayoutParams ownerInfoLayoutParams = (LinearLayout.LayoutParams) this.mOwnerInfo.getLayoutParams();
        ownerInfoLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_owner_info_top_margin);
        this.mOwnerInfo.setLayoutParams(ownerInfoLayoutParams);
        LinearLayout.LayoutParams lockScreenMagazineInfoLayoutParams = (LinearLayout.LayoutParams) this.mLockScreenMagazineInfo.getLayoutParams();
        lockScreenMagazineInfoLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_owner_info_top_margin);
        this.mLockScreenMagazineInfo.setLayoutParams(lockScreenMagazineInfoLayoutParams);
    }

    public void updateClockView(boolean hasNotifiction, boolean isUnderKeyguard) {
        if (!isUnderKeyguard) {
            return;
        }
        if (!isSupportVerticalClock()) {
            this.mHasNotification = hasNotifiction;
            showHorizontalTime();
        } else if (!this.mHasNotification || hasNotifiction) {
            this.mHasNotification = hasNotifiction;
            this.mHandler.removeMessages(0);
            updateClockView();
        } else {
            this.mHasNotification = false;
            this.mHandler.sendEmptyMessageDelayed(0, 200);
        }
    }

    public void setClockAlpha(float alpha) {
        setAlpha(alpha);
    }

    public void updateClockView() {
        if (!isSupportVerticalClock() || this.mInSmartCoverSmallWindowMode || this.mHasNotification) {
            showHorizontalTime();
        } else {
            showVerticalTime();
        }
    }

    private void showHorizontalTime() {
        if (!this.mVerticalToHorizontalAnim.isRunning()) {
            float translationExtra = this.mVerticalTimeLayoutHeight - this.mHorizontalTimeLayoutHeight;
            clearAnim();
            if (!isSupportVerticalClock() || this.mInSmartCoverSmallWindowMode || !this.mPowerManager.isScreenOn() || this.mShowHorizontalTime || this.mSkipAnimation) {
                this.mShowHorizontalTime = true;
                this.mHorizontalTimeLayout.setAlpha(1.0f);
                this.mHorizontalTimeLayout.setScaleX(1.0f);
                this.mHorizontalTimeLayout.setScaleY(1.0f);
                this.mHorizontalTimeLayout.setTranslationY(0.0f);
                this.mVerticalMin.setAlpha(0.0f);
                this.mVerticalHour.setAlpha(0.0f);
                this.mDateAndBatteryInfoLayout.setTranslationY(-translationExtra);
                this.mLunarCalendarInfo.setTranslationY(-translationExtra);
                this.mOwnerInfo.setTranslationY(-translationExtra);
                this.mLockScreenMagazineInfo.setTranslationY(-getLockScreenMagazineInfoTranslationY());
                handleNotificationChange();
            } else {
                playVerticalToHorizontalAnim();
            }
            this.mHorizontalTimeLayout.setImportantForAccessibility(1);
            this.mVerticalTimeLayout.setImportantForAccessibility(2);
        }
    }

    private float getLockScreenMagazineInfoTranslationY() {
        boolean lunarInfoExist = false;
        boolean ownerInfoExist = this.mOwnerInfo.getVisibility() == 0;
        if (this.mLunarCalendarInfo.getVisibility() == 0) {
            lunarInfoExist = true;
        }
        float translationExtra = this.mVerticalTimeLayoutHeight - this.mHorizontalTimeLayoutHeight;
        if (!this.mHasNotification) {
            return translationExtra;
        }
        if (ownerInfoExist) {
            translationExtra += (float) this.mOwnerInfoHeight;
        }
        if (lunarInfoExist) {
            return translationExtra + ((float) this.mLunarCalendarInfoHeight);
        }
        return translationExtra;
    }

    private void showVerticalTime() {
        if (!this.mHorizontalToVerticalAnim.isRunning()) {
            clearAnim();
            if (!this.mPowerManager.isScreenOn() || !this.mShowHorizontalTime || this.mSkipAnimation) {
                this.mShowHorizontalTime = false;
                this.mVerticalMin.setAlpha(1.0f);
                this.mVerticalHour.setAlpha(1.0f);
                this.mVerticalMin.setTranslationY(0.0f);
                this.mVerticalHour.setTranslationY(0.0f);
                this.mHorizontalTimeLayout.setAlpha(0.0f);
                this.mDateAndBatteryInfoLayout.setTranslationY(0.0f);
                this.mLunarCalendarInfo.setTranslationY(0.0f);
                this.mLunarCalendarInfo.setAlpha(1.0f);
                this.mOwnerInfo.setTranslationY(0.0f);
                this.mOwnerInfo.setAlpha(1.0f);
                this.mLockScreenMagazineInfo.setTranslationY(0.0f);
                this.mLockScreenMagazineInfo.setAlpha(1.0f);
            } else {
                playHorizontalToVerticalAnim();
            }
            this.mHorizontalTimeLayout.setImportantForAccessibility(2);
            this.mVerticalTimeLayout.setImportantForAccessibility(1);
        }
    }

    /* access modifiers changed from: protected */
    public void clearAnim() {
        this.mHorizontalToVerticalAnim.cancel();
        this.mVerticalToHorizontalAnim.cancel();
        this.mHorizontalTimeLayout.clearAnimation();
        this.mCurrentDate.clearAnimation();
        this.mVerticalMin.clearAnimation();
        this.mVerticalHour.clearAnimation();
    }

    public void playVerticalToHorizontalAnim() {
        float translationY = (((this.mVerticalTimeLayoutHeight - this.mVerticalTimePaddingTop) / 2.0f) + this.mVerticalTimePaddingTop) - (((this.mHorizontalTimeLayoutHeight - this.mHorizontalTimePaddingTop) / 2.0f) + this.mHorizontalTimePaddingTop);
        float translationExtra = this.mVerticalTimeLayoutHeight - this.mHorizontalTimeLayoutHeight;
        ObjectAnimator minUp = ObjectAnimator.ofFloat(this.mVerticalMin, "translationY", new float[]{0.0f, -translationY});
        minUp.setDuration(425);
        minUp.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator minAlphaOut = ObjectAnimator.ofFloat(this.mVerticalMin, "alpha", new float[]{1.0f, 0.0f});
        minAlphaOut.setDuration(210);
        minAlphaOut.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator hourUp = ObjectAnimator.ofFloat(this.mVerticalHour, "translationY", new float[]{0.0f, -translationY});
        hourUp.setDuration(425);
        hourUp.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator hourAlphaOut = ObjectAnimator.ofFloat(this.mVerticalHour, "alpha", new float[]{1.0f, 0.0f});
        hourAlphaOut.setDuration(210);
        hourAlphaOut.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator horizontalTimeScaleX = ObjectAnimator.ofFloat(this.mHorizontalTimeLayout, "scaleX", new float[]{0.5f, 1.0f});
        horizontalTimeScaleX.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator horizontalTimeScaleY = ObjectAnimator.ofFloat(this.mHorizontalTimeLayout, "scaleY", new float[]{0.5f, 1.0f});
        horizontalTimeScaleY.setDuration(425);
        horizontalTimeScaleY.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator horizontalTimeUp = ObjectAnimator.ofFloat(this.mHorizontalTimeLayout, "translationY", new float[]{translationY / 2.0f, 0.0f});
        horizontalTimeUp.setDuration(425);
        horizontalTimeUp.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator horizontalTimeAlpha = ObjectAnimator.ofFloat(this.mHorizontalTimeLayout, "alpha", new float[]{0.0f, 1.0f});
        horizontalTimeAlpha.setDuration(425);
        ObjectAnimator dateUp = ObjectAnimator.ofFloat(this.mDateAndBatteryInfoLayout, "translationY", new float[]{0.0f, -translationExtra});
        dateUp.setDuration(425);
        dateUp.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator lunarCalendarInfoUp = ObjectAnimator.ofFloat(this.mLunarCalendarInfo, "translationY", new float[]{0.0f, -translationExtra});
        lunarCalendarInfoUp.setDuration(425);
        lunarCalendarInfoUp.setInterpolator(Ease.Cubic.easeInOut);
        float f = translationY;
        ObjectAnimator lunarCalendarInfoOutAlpha = ObjectAnimator.ofFloat(this.mLunarCalendarInfo, "alpha", new float[]{1.0f, 0.0f});
        lunarCalendarInfoOutAlpha.setDuration(210);
        lunarCalendarInfoOutAlpha.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator lunarCalendarInfoOutAlpha2 = lunarCalendarInfoOutAlpha;
        ObjectAnimator ownerInfoUp = ObjectAnimator.ofFloat(this.mOwnerInfo, "translationY", new float[]{0.0f, -translationExtra});
        ownerInfoUp.setDuration(425);
        ownerInfoUp.setInterpolator(Ease.Cubic.easeInOut);
        float f2 = translationExtra;
        ObjectAnimator ownerInfoOutAlpha = ObjectAnimator.ofFloat(this.mOwnerInfo, "alpha", new float[]{1.0f, 0.0f});
        ownerInfoOutAlpha.setDuration(210);
        ownerInfoOutAlpha.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator ownerInfoOutAlpha2 = ownerInfoOutAlpha;
        ObjectAnimator lockScreenMagazineInfoUp = ObjectAnimator.ofFloat(this.mLockScreenMagazineInfo, "translationY", new float[]{0.0f, -getLockScreenMagazineInfoTranslationY()});
        lockScreenMagazineInfoUp.setDuration(425);
        lockScreenMagazineInfoUp.setInterpolator(Ease.Cubic.easeInOut);
        this.mVerticalToHorizontalAnim.play(minUp).with(minAlphaOut).with(hourUp).with(hourAlphaOut).with(horizontalTimeScaleX).with(horizontalTimeScaleY).with(horizontalTimeAlpha).with(horizontalTimeUp).with(dateUp).with(lunarCalendarInfoUp).with(lunarCalendarInfoOutAlpha2).with(ownerInfoUp).with(ownerInfoOutAlpha2).with(lockScreenMagazineInfoUp);
        ObjectAnimator objectAnimator = ownerInfoUp;
        this.mVerticalToHorizontalAnim.addListener(new AnimatorListenerAdapter() {
            boolean isCanceled = false;

            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                this.isCanceled = true;
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!this.isCanceled) {
                    boolean unused = MiuiKeyguardVerticalClock.this.mShowHorizontalTime = true;
                }
            }
        });
        this.mVerticalToHorizontalAnim.start();
    }

    public void playHorizontalToVerticalAnim() {
        float translationY = (((this.mVerticalTimeLayoutHeight - this.mVerticalTimePaddingTop) / 2.0f) + this.mVerticalTimePaddingTop) - (((this.mHorizontalTimeLayoutHeight - this.mHorizontalTimePaddingTop) / 2.0f) + this.mHorizontalTimePaddingTop);
        float translationExtra = this.mVerticalTimeLayoutHeight - this.mHorizontalTimeLayoutHeight;
        ObjectAnimator minDown = ObjectAnimator.ofFloat(this.mVerticalMin, "translationY", new float[]{-translationY, 0.0f});
        minDown.setDuration(425);
        minDown.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator minAlphaIn = ObjectAnimator.ofFloat(this.mVerticalMin, "alpha", new float[]{0.0f, 1.0f});
        minAlphaIn.setDuration(425);
        minAlphaIn.setInterpolator(Ease.Sine.easeInOut);
        ObjectAnimator hourDown = ObjectAnimator.ofFloat(this.mVerticalHour, "translationY", new float[]{-translationY, 0.0f});
        hourDown.setDuration(425);
        hourDown.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator hourAlphaIn = ObjectAnimator.ofFloat(this.mVerticalHour, "alpha", new float[]{0.0f, 1.0f});
        hourAlphaIn.setDuration(425);
        hourAlphaIn.setInterpolator(Ease.Sine.easeInOut);
        ObjectAnimator horizontalTimeTranslationY = ObjectAnimator.ofFloat(this.mHorizontalTimeLayout, "translationY", new float[]{0.0f, translationY});
        horizontalTimeTranslationY.setDuration(425);
        horizontalTimeTranslationY.setInterpolator(Ease.Cubic.easeInOut);
        ObjectAnimator horizontalTimeAlphaOut = ObjectAnimator.ofFloat(this.mHorizontalTimeLayout, "alpha", new float[]{1.0f, 0.0f});
        horizontalTimeAlphaOut.setDuration(210);
        horizontalTimeAlphaOut.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator dateDown = ObjectAnimator.ofFloat(this.mDateAndBatteryInfoLayout, "translationY", new float[]{-translationExtra, 0.0f});
        dateDown.setDuration(425);
        dateDown.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator lunarCalendarInfoDown = ObjectAnimator.ofFloat(this.mLunarCalendarInfo, "translationY", new float[]{-translationExtra, 0.0f});
        lunarCalendarInfoDown.setDuration(425);
        lunarCalendarInfoDown.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator luncarCalendarInfoInAlpha = ObjectAnimator.ofFloat(this.mLunarCalendarInfo, "alpha", new float[]{0.0f, 1.0f});
        luncarCalendarInfoInAlpha.setDuration(425);
        luncarCalendarInfoInAlpha.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator ownerInfoDown = ObjectAnimator.ofFloat(this.mOwnerInfo, "translationY", new float[]{-translationExtra, 0.0f});
        ownerInfoDown.setDuration(425);
        ownerInfoDown.setInterpolator(Ease.Cubic.easeOut);
        float f = translationY;
        ObjectAnimator ownerInfoInAlpha = ObjectAnimator.ofFloat(this.mOwnerInfo, "alpha", new float[]{0.0f, 1.0f});
        ownerInfoInAlpha.setDuration(425);
        ownerInfoInAlpha.setInterpolator(Ease.Cubic.easeOut);
        ObjectAnimator lockScreenMagazineInfoDown = ObjectAnimator.ofFloat(this.mLockScreenMagazineInfo, "translationY", new float[]{-getLockScreenMagazineInfoTranslationY(), 0.0f});
        lockScreenMagazineInfoDown.setDuration(425);
        lockScreenMagazineInfoDown.setInterpolator(Ease.Cubic.easeOut);
        this.mHorizontalToVerticalAnim.play(minDown).with(minAlphaIn).with(hourDown).with(hourAlphaIn).with(horizontalTimeTranslationY).with(horizontalTimeAlphaOut).with(dateDown).with(lunarCalendarInfoDown).with(luncarCalendarInfoInAlpha).with(ownerInfoDown).with(ownerInfoInAlpha).with(lockScreenMagazineInfoDown);
        this.mHorizontalToVerticalAnim.addListener(new AnimatorListenerAdapter() {
            boolean isCanceled = false;

            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                this.isCanceled = true;
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!this.isCanceled) {
                    boolean unused = MiuiKeyguardVerticalClock.this.mShowHorizontalTime = false;
                }
            }
        });
        this.mHorizontalToVerticalAnim.start();
    }

    public void updateTimeAndBatteryInfo() {
        updateBatteryInfoAndDate();
        updateTime();
    }

    public float getClockVisibleHeight() {
        float height = ((float) getClockHeight()) - (this.mVerticalTimeLayoutHeight - this.mHorizontalTimeLayoutHeight);
        if (this.mOwnerInfo != null && this.mOwnerInfo.getVisibility() == 0) {
            height -= (float) this.mOwnerInfoHeight;
        }
        if (this.mLunarCalendarInfo != null && this.mLunarCalendarInfo.getVisibility() == 0) {
            height -= (float) this.mLunarCalendarInfoHeight;
        }
        if (height > 0.0f) {
            return height;
        }
        return 0.0f;
    }
}
