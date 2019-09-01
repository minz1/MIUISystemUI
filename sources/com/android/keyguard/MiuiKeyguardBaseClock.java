package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.security.MiuiLockPatternUtils;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.keyguard.Ease;
import com.android.keyguard.KeyguardClockContainer;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.R;
import java.util.Locale;
import java.util.TimeZone;
import miui.date.Calendar;
import miui.os.Build;
import miui.util.FeatureParser;

public class MiuiKeyguardBaseClock extends LinearLayout implements KeyguardClockContainer.KeyguardClockView {
    protected boolean m24HourFormat;
    protected TextView mBatteryInfo;
    private final Runnable mBatteryInfoAndDateTransition;
    protected Calendar mCalendar;
    private int mCalendarDayOfWeek;
    protected Context mContext;
    protected TextView mCurrentDate;
    protected boolean mDarkMode;
    protected FrameLayout mDateAndBatteryInfoLayout;
    protected int mDensityDpi;
    protected float mFontScale;
    protected boolean mFontScaleChanged;
    protected final Handler mHandler;
    protected boolean mHasNotification;
    protected boolean mInSmartCoverSmallWindowMode;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    protected String mLanguage;
    protected String mLastOwnerInfoString;
    protected MiuiLockPatternUtils mLockPatternUtils;
    protected TextView mLockScreenMagazineInfo;
    protected TextView mLunarCalendarInfo;
    protected int mLunarCalendarInfoHeight;
    protected boolean mOldHasNotification;
    protected TextView mOwnerInfo;
    protected int mOwnerInfoHeight;
    protected String mOwnerInfoString;
    protected Resources mResources;
    protected int mSelectedClockPosition;
    protected boolean mShowBatteryInfo;
    private final ContentObserver mSmartCoverSettingsContentObserver;
    protected Typeface mThinFontTypeface;

    public MiuiKeyguardBaseClock(Context context) {
        this(context, null);
    }

    public MiuiKeyguardBaseClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = null;
        this.mResources = null;
        this.mOwnerInfoString = null;
        this.mLastOwnerInfoString = null;
        this.mThinFontTypeface = null;
        this.mDarkMode = false;
        this.mOldHasNotification = false;
        this.mHasNotification = false;
        this.mOwnerInfoHeight = 0;
        this.mLunarCalendarInfoHeight = 0;
        this.mSelectedClockPosition = 0;
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    MiuiKeyguardBaseClock.this.updateClockView();
                }
            }
        };
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onKeyguardVisibilityChanged(boolean showing) {
                if (showing) {
                    MiuiKeyguardBaseClock.this.updateHourFormat();
                    MiuiKeyguardBaseClock.this.updateOwnerInfo();
                    MiuiKeyguardBaseClock.this.updateLunarCalendarInfo();
                }
            }

            public void onUserSwitchComplete(int userId) {
                MiuiKeyguardBaseClock.this.updateOwnerInfo();
            }

            public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
                MiuiKeyguardBaseClock.this.updateBatteryLevelText(status);
            }

            public void onLockScreenMagazineStatusChanged() {
                MiuiKeyguardBaseClock.this.updateLockScreenMagazineInfo();
            }

            public void onRegionChanged() {
                MiuiKeyguardBaseClock.this.updateLockScreenMagazineInfo();
            }

            public void onUserUnlocked() {
                MiuiKeyguardBaseClock.this.updateLockScreenMagazineInfo();
            }
        };
        this.mSmartCoverSettingsContentObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                MiuiKeyguardBaseClock.this.mInSmartCoverSmallWindowMode = MiuiSettings.System.isInSmallWindowMode(MiuiKeyguardBaseClock.this.mContext);
                MiuiKeyguardBaseClock.this.updateOwnerInfo();
                MiuiKeyguardBaseClock.this.updateClockView();
            }
        };
        this.mBatteryInfoAndDateTransition = new Runnable() {
            public void run() {
                final Animation dateFadeIn = AnimationUtils.loadAnimation(MiuiKeyguardBaseClock.this.mContext, 17432576);
                Animation batteryInfoFadeOut = AnimationUtils.loadAnimation(MiuiKeyguardBaseClock.this.mContext, 17432577);
                dateFadeIn.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                        MiuiKeyguardBaseClock.this.mCurrentDate.setVisibility(0);
                    }

                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                batteryInfoFadeOut.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                        MiuiKeyguardBaseClock.this.mBatteryInfo.setVisibility(4);
                        MiuiKeyguardBaseClock.this.mCurrentDate.startAnimation(dateFadeIn);
                    }

                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                MiuiKeyguardBaseClock.this.mBatteryInfo.startAnimation(batteryInfoFadeOut);
            }
        };
        this.mContext = context;
        this.mResources = this.mContext.getResources();
        this.mThinFontTypeface = Typeface.createFromAsset(this.mContext.getAssets(), "fonts/Mitype2018-clock2.ttf");
        this.mLockPatternUtils = new MiuiLockPatternUtils(this.mContext);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mSelectedClockPosition = Settings.System.getIntForUser(contentResolver, "selected_keyguard_clock_position", 0, KeyguardUpdateMonitor.getCurrentUser());
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDateAndBatteryInfoLayout = (FrameLayout) findViewById(R.id.date_and_battery_info);
        this.mCurrentDate = (TextView) findViewById(R.id.current_date);
        this.mBatteryInfo = (TextView) findViewById(R.id.unlock_screen_battery_info);
        this.mOwnerInfo = (TextView) findViewById(R.id.unlock_screen_owner_info);
        this.mLunarCalendarInfo = (TextView) findViewById(R.id.unlock_screen_lunar_calendar_info);
        updateLunarCalendarInfoHeight();
        this.mLockScreenMagazineInfo = (TextView) findViewById(R.id.unlock_screen_lock_screen_magazine_info);
        this.mLockScreenMagazineInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LockScreenMagazineUtils.gotoLockScreenMagazine(MiuiKeyguardBaseClock.this.mContext, "lockScreenInfo");
                LockScreenMagazineAnalytics.recordLockScreenMagazinePreviewAction(MiuiKeyguardBaseClock.this.mContext, "click_entry");
            }
        });
        MiuiKeyguardUtils.setViewTouchDelegate(this.mLockScreenMagazineInfo, 30);
        this.mCalendar = new Calendar();
        updateHourFormat();
        this.mInSmartCoverSmallWindowMode = MiuiSettings.System.isInSmallWindowMode(this.mContext);
        updateOwnerInfo();
        updateLunarCalendarInfo();
        updateLockScreenMagazineInfo();
    }

    public void updateHourFormat() {
        this.m24HourFormat = DateFormat.is24HourFormat(this.mContext, KeyguardUpdateMonitor.getCurrentUser());
    }

    public void updateOwnerInfo() {
        if (this.mOwnerInfo != null) {
            this.mOwnerInfoString = KeyguardCompatibilityHelperForN.getOwnerInfo(this.mLockPatternUtils, KeyguardUpdateMonitor.getCurrentUser());
            if (!TextUtils.isEmpty(this.mOwnerInfoString)) {
                this.mOwnerInfo.setVisibility(0);
                if (!this.mOwnerInfoString.equals(this.mLastOwnerInfoString)) {
                    this.mLastOwnerInfoString = this.mOwnerInfoString;
                    this.mOwnerInfo.setText(this.mOwnerInfoString);
                    updateOwnerInfoHeight();
                }
            } else {
                this.mOwnerInfo.setVisibility(8);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void clearAnim() {
    }

    /* access modifiers changed from: protected */
    public void updateOwnerInfoHeight() {
        this.mOwnerInfo.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                if (height > 0) {
                    MiuiKeyguardBaseClock.this.mOwnerInfoHeight = ((int) MiuiKeyguardBaseClock.this.mResources.getDimension(R.dimen.keyguard_owner_info_top_margin)) + height;
                    MiuiKeyguardBaseClock.this.clearAnim();
                    MiuiKeyguardBaseClock.this.updateClockView();
                    MiuiKeyguardBaseClock.this.mOwnerInfo.removeOnLayoutChangeListener(this);
                }
            }
        });
    }

    public void updateLunarCalendarInfo() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "show_lunar_calendar", 0, KeyguardUpdateMonitor.getCurrentUser()) != 1) {
            z = false;
        }
        boolean showLunarCalendar = z;
        if (!Locale.CHINESE.getLanguage().equals(Locale.getDefault().getLanguage()) || !showLunarCalendar) {
            this.mLunarCalendarInfo.setVisibility(8);
            return;
        }
        Calendar calendar = new Calendar();
        this.mLunarCalendarInfo.setVisibility(0);
        this.mLunarCalendarInfo.setText(calendar.format("YY年 N月e"));
    }

    /* access modifiers changed from: protected */
    public void updateLunarCalendarInfoHeight() {
        this.mLunarCalendarInfoHeight = this.mLunarCalendarInfo.getLineHeight() + ((int) this.mResources.getDimension(R.dimen.keyguard_owner_info_top_margin));
    }

    public void updateLockScreenMagazineInfo() {
        String info = null;
        if (LockScreenMagazineUtils.isLockScreenMagazineAvailable(this.mContext) && WallpaperAuthorityUtils.isLockScreenMagazineWallpaper() && KeyguardUpdateMonitor.getInstance(this.mContext).isLockScreenMagazinePkgExist() && !Build.IS_INTERNATIONAL_BUILD) {
            info = getLockScreenMagazineInfo();
        }
        if (!TextUtils.isEmpty(info)) {
            this.mLockScreenMagazineInfo.setText(info);
            this.mLockScreenMagazineInfo.setVisibility(0);
            MiuiKeyguardUtils.setViewTouchDelegate(this.mLockScreenMagazineInfo, 30);
            return;
        }
        this.mLockScreenMagazineInfo.setVisibility(8);
        MiuiKeyguardUtils.setViewTouchDelegate(this.mLockScreenMagazineInfo, 0);
    }

    public void setSelectedClockPosition(int clockPosition) {
        this.mSelectedClockPosition = clockPosition;
        updateClockView();
    }

    /* access modifiers changed from: protected */
    public boolean isSupportVerticalClock() {
        return MiuiKeyguardUtils.isSupportVerticalClock(this.mSelectedClockPosition, this.mContext);
    }

    public float getTopMargin() {
        return 0.0f;
    }

    private String getLockScreenMagazineInfo() {
        LockScreenMagazineWallpaperInfo lockScreenMagazineWallpaperInfo = KeyguardUpdateMonitor.getInstance(this.mContext).getLockScreenMagazineWallpaperInfo();
        if (lockScreenMagazineWallpaperInfo == null) {
            return null;
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isSupportLockScreenMagazineLeft()) {
            if (lockScreenMagazineWallpaperInfo.isTitleCustomized) {
                if (!TextUtils.isEmpty(lockScreenMagazineWallpaperInfo.title)) {
                    return lockScreenMagazineWallpaperInfo.title;
                }
                return null;
            } else if (TextUtils.isEmpty(lockScreenMagazineWallpaperInfo.entryTitle) || !Locale.CHINESE.getLanguage().equals(Locale.getDefault().getLanguage())) {
                return null;
            } else {
                return lockScreenMagazineWallpaperInfo.entryTitle;
            }
        } else if (TextUtils.isEmpty(lockScreenMagazineWallpaperInfo.title) || !Locale.CHINESE.getLanguage().equals(Locale.getDefault().getLanguage())) {
            return null;
        } else {
            return lockScreenMagazineWallpaperInfo.title;
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("is_small_window"), false, this.mSmartCoverSettingsContentObserver);
        setDarkMode(this.mDarkMode);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
    }

    public void setDarkMode(boolean darkMode) {
        this.mDarkMode = darkMode;
        int color = darkMode ? getContext().getResources().getColor(R.color.miui_common_unlock_screen_common_time_dark_text_color) : -1;
        this.mCurrentDate.setTextColor(color);
        this.mBatteryInfo.setTextColor(color);
        this.mLunarCalendarInfo.setTextColor(color);
        this.mOwnerInfo.setTextColor(color);
        this.mLockScreenMagazineInfo.setTextColor(color);
        updateTime();
        updateDrawableResources();
    }

    public int getClockHeight() {
        return getMeasuredHeight();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mContext.getContentResolver().unregisterContentObserver(this.mSmartCoverSettingsContentObserver);
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float fontScale = newConfig.fontScale;
        if (this.mFontScale != fontScale) {
            this.mFontScaleChanged = true;
            updateViewsTextSize();
            this.mFontScale = fontScale;
        }
        int densityDpi = newConfig.densityDpi;
        if (this.mDensityDpi != densityDpi) {
            this.mFontScaleChanged = true;
            updateViewsTextSize();
            updateViewsLayoutParams();
            updateDrawableResources();
            this.mDensityDpi = densityDpi;
        }
        String language = newConfig.locale.getLanguage();
        if (!TextUtils.isEmpty(language) && !language.equals(this.mLanguage)) {
            updateLockScreenMagazineInfo();
            this.mLanguage = language;
        }
    }

    public void updateTimeZone(String timeZone) {
        if (!TextUtils.isEmpty(timeZone)) {
            this.mCalendar = new Calendar(TimeZone.getTimeZone(timeZone));
            updateTime();
        }
    }

    public void updateResidentTimeZone(String residentTimezone) {
    }

    public void updateTime() {
        this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        this.mCurrentDate.setText(this.mCalendar.format(this.mContext.getString(this.m24HourFormat ? R.string.lock_screen_date : R.string.lock_screen_date_12)));
        int day = this.mCalendar.get(14);
        if (day != this.mCalendarDayOfWeek) {
            updateLunarCalendarInfo();
            this.mCalendarDayOfWeek = day;
        }
    }

    /* access modifiers changed from: protected */
    public void updateDrawableResources() {
        int i;
        Resources resources = this.mResources;
        if (this.mDarkMode) {
            i = R.drawable.keyguard_bottom_guide_right_arrow_dark;
        } else {
            i = R.drawable.keyguard_bottom_guide_right_arrow;
        }
        this.mLockScreenMagazineInfo.setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(i), null);
    }

    /* access modifiers changed from: protected */
    public void updateViewsTextSize() {
    }

    /* access modifiers changed from: protected */
    public void updateViewsLayoutParams() {
    }

    public void updateBatteryInfoAndDate() {
        if (!this.mShowBatteryInfo || !FeatureParser.getBoolean("is_pad", false)) {
            this.mCurrentDate.setVisibility(0);
            this.mBatteryInfo.setVisibility(4);
            return;
        }
        this.mCurrentDate.setVisibility(4);
        this.mBatteryInfo.setVisibility(0);
        this.mCurrentDate.clearAnimation();
        this.mBatteryInfo.clearAnimation();
        this.mBatteryInfo.removeCallbacks(this.mBatteryInfoAndDateTransition);
        this.mBatteryInfo.postDelayed(this.mBatteryInfoAndDateTransition, 2000);
    }

    /* access modifiers changed from: private */
    public void updateBatteryLevelText(KeyguardUpdateMonitor.BatteryStatus status) {
        if (status.isPluggedIn() || status.isBatteryLow()) {
            this.mShowBatteryInfo = true;
            if (status.isPluggedIn()) {
                if (status.isCharged()) {
                    this.mBatteryInfo.setText(R.string.unlockscreen_recharge_completed);
                    return;
                }
                this.mBatteryInfo.setText(getResources().getString(R.string.unlockscreen_recharging_message, new Object[]{Integer.valueOf(status.level)}));
            } else if (status.isBatteryLow()) {
                this.mBatteryInfo.setText(R.string.unlockscreen_low_battery);
            }
        } else {
            this.mShowBatteryInfo = false;
        }
    }

    public void updateClockView(boolean hasNotifiction, boolean isUnderKeyguard) {
    }

    public void setClockAlpha(float alpha) {
        setAlpha(alpha);
    }

    public void updateClockView() {
    }

    /* access modifiers changed from: protected */
    public void handleNotificationChange() {
        float f = 0.0f;
        if (this.mHasNotification == this.mOldHasNotification || isSupportVerticalClock()) {
            this.mLunarCalendarInfo.setAlpha(this.mHasNotification ? 0.0f : 1.0f);
            TextView textView = this.mOwnerInfo;
            if (!this.mHasNotification) {
                f = 1.0f;
            }
            textView.setAlpha(f);
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        TextView textView2 = this.mLunarCalendarInfo;
        Property property = ALPHA;
        float[] fArr = new float[2];
        fArr[0] = this.mHasNotification ? 1.0f : 0.0f;
        fArr[1] = this.mHasNotification ? 0.0f : 1.0f;
        ObjectAnimator lunarCalendarInfoAlpha = ObjectAnimator.ofFloat(textView2, property, fArr);
        TextView textView3 = this.mOwnerInfo;
        Property property2 = ALPHA;
        float[] fArr2 = new float[2];
        fArr2[0] = this.mHasNotification ? 1.0f : 0.0f;
        if (!this.mHasNotification) {
            f = 1.0f;
        }
        fArr2[1] = f;
        animatorSet.playTogether(new Animator[]{lunarCalendarInfoAlpha, ObjectAnimator.ofFloat(textView3, property2, fArr2)});
        animatorSet.setDuration(250);
        animatorSet.setInterpolator(Ease.Cubic.easeInOut);
        animatorSet.start();
        this.mOldHasNotification = this.mHasNotification;
    }

    public void updateTimeAndBatteryInfo() {
        updateBatteryInfoAndDate();
        updateTime();
    }

    public float getClockVisibleHeight() {
        float height = (float) getHeight();
        if (this.mOwnerInfo != null && this.mOwnerInfo.getVisibility() == 0) {
            height -= (float) this.mOwnerInfo.getHeight();
        }
        if (this.mLunarCalendarInfo != null && this.mLunarCalendarInfo.getVisibility() == 0) {
            height -= (float) this.mLunarCalendarInfo.getHeight();
        }
        if (height > 0.0f) {
            return height;
        }
        return 0.0f;
    }
}
