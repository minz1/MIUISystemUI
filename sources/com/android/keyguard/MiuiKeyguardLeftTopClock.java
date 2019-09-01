package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Constants;
import com.android.systemui.R;
import miui.date.DateUtils;

public class MiuiKeyguardLeftTopClock extends MiuiKeyguardBaseClock {
    private TextView mTimeText;

    public MiuiKeyguardLeftTopClock(Context context) {
        this(context, null);
    }

    public MiuiKeyguardLeftTopClock(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeText = (TextView) findViewById(R.id.current_time);
        updateViewsForNotch();
    }

    /* access modifiers changed from: protected */
    public void updateViewsForNotch() {
        if (Constants.IS_NOTCH) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.mTimeText.getLayoutParams();
            lp.topMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.left_top_clock_margin_top_for_notch);
            this.mTimeText.setLayoutParams(lp);
        }
    }

    public void setDarkMode(boolean darkMode) {
        super.setDarkMode(darkMode);
        this.mTimeText.setTextColor(darkMode ? getContext().getResources().getColor(R.color.miui_common_unlock_screen_common_time_dark_text_color) : -1);
    }

    public int getClockHeight() {
        return getHeight();
    }

    public float getTopMargin() {
        return (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.left_top_clock_margin_top);
    }

    public void updateTime() {
        super.updateTime();
        this.mTimeText.setText(DateUtils.formatDateTime(System.currentTimeMillis(), (this.m24HourFormat ? 32 : 16) | 12 | 64));
    }

    public void updateClockView(boolean hasNotifiction, boolean isUnderKeyguard) {
        if (isUnderKeyguard) {
            this.mHasNotification = hasNotifiction;
            handleNotificationChange();
            this.mLockScreenMagazineInfo.setTranslationY(this.mHasNotification ? -getLockScreenMagazineInfoTranslationY() : 0.0f);
        }
    }

    public void setClockAlpha(float alpha) {
        setAlpha(alpha);
    }

    private float getLockScreenMagazineInfoTranslationY() {
        boolean lunarInfoExist = false;
        boolean ownerInfoExist = this.mOwnerInfo.getVisibility() == 0;
        if (this.mLunarCalendarInfo.getVisibility() == 0) {
            lunarInfoExist = true;
        }
        float translationExtra = 0.0f;
        if (!this.mHasNotification) {
            return 0.0f;
        }
        if (ownerInfoExist) {
            translationExtra = 0.0f + ((float) this.mOwnerInfoHeight);
        }
        if (lunarInfoExist) {
            return translationExtra + ((float) this.mLunarCalendarInfoHeight);
        }
        return translationExtra;
    }

    public float getClockVisibleHeight() {
        float height = (float) getHeight();
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
